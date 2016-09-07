/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;
// above package assignment temporary pending better aysnch release process
// package org.dspace.ctask.integrity;

import com.atmire.utils.ItemUtils;
import com.atmire.utils.MetadataUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Suspendable;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.*;

/**  ClamScan.java
 *
 * A set of methods to scan using the
 * clamav daemon.
 *
 * TODO: add a check for the inputstream size limit
 *
 * @author wbossons
 */

@Suspendable(invoked = Curator.Invoked.INTERACTIVE)
public class ClamScan extends AbstractCurationTask {
    private static final int DEFAULT_CHUNK_SIZE = 4096;//2048
    private static final byte[] INSTREAM = "zINSTREAM\0".getBytes();
    private static final byte[] PING = "zPING\0".getBytes();
    private static final byte[] STATS = "nSTATS\n".getBytes();//prefix with z
    private static final byte[] IDSESSION = "zIDSESSION\0".getBytes();
    private static final byte[] END = "zEND\0".getBytes();
    private static final String PLUGIN_PREFIX = "clamav";
    private static final String INFECTED_MESSAGE = "had virus detected.";
    private static final String CLEAN_MESSAGE = "had no viruses detected.";
    private static final String CONNECT_FAIL_MESSAGE = "Unable to connect to virus service - check setup";
    private static final String SCAN_FAIL_MESSAGE = "Error encountered using virus service - check setup";
    private static final String NEW_ITEM_HANDLE = "in workflow";
    private static final int FILE_NOT_FOUND = 10;

    private static final Logger log = Logger.getLogger(ClamScan.class);
    private final Context context;
    private final boolean commit;

    private int status = Curator.CURATE_UNSET;
    private List<String> results = null;

    private Socket socket = null;
    private DataOutputStream dataOutputStream = null;

    public ClamScan(Context context, boolean commit) {
        this.context = context;
        this.commit = commit;
    }

    public ClamScan() {
        this(null, true);
    }

    public static String getHost() {
        return ConfigurationManager.getProperty(PLUGIN_PREFIX, "service.host");
    }

    public static int getPort() {
        return ConfigurationManager.getIntProperty(PLUGIN_PREFIX, "service.port");
    }

    public static int getTimeout() {
        return ConfigurationManager.getIntProperty(PLUGIN_PREFIX, "socket.timeout");
    }

    public static boolean isFailfast() {
        return ConfigurationManager.getBooleanProperty(PLUGIN_PREFIX, "scan.failfast");
    }

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        status = Curator.CURATE_SKIP;
        logDebugMessage("The target dso is " + dso.getName());

        Item item = null;
        List<Bitstream> bitstreams = new LinkedList<>();
        results = new ArrayList<>();
        status = Curator.CURATE_SUCCESS;

        if (dso instanceof Item) {
            item = (Item) dso;

            try {
                Collections.addAll(bitstreams, item.getNonInternalBitstreams());
            } catch (SQLException authE) {
                throw new IOException(authE.getMessage(), authE);
            }
        }

        if (dso instanceof Bitstream) {
            Bitstream bitstream = (Bitstream) dso;
            bitstreams.add(bitstream);

            item = ItemUtils.getItem(bitstream);
        }

        if (CollectionUtils.isNotEmpty(bitstreams)) {

            try {
                openSession();
            } catch (IOException ioE) {
                // no point going further - set result and error out
                closeSession();
                setResult(CONNECT_FAIL_MESSAGE);
                return Curator.CURATE_ERROR;
            }

            try {
                String itemHandle;
                if (item == null)
                    itemHandle = "null (bitstream id:" + dso.getID() + ")";
                else
                    itemHandle = getItemHandle(item);

                boolean breakBitstreamIteration = false;
                Iterator<Bitstream> iterator = bitstreams.iterator();
                while (iterator.hasNext() && !breakBitstreamIteration) {
                    Bitstream next = iterator.next();
                    breakBitstreamIteration = performBitstream(itemHandle, next);
                }

                if (status != Curator.CURATE_ERROR) {
                    formatResults(itemHandle);
                }
            } catch (SQLException | AuthorizeException e) {
                throw new IOException(e.getMessage(), e);
            } finally {
                closeSession();
            }

        }

        return status;
    }

    public boolean performBitstream(String itemHandle, Bitstream bitstream)
            throws SQLException, AuthorizeException, IOException {
        boolean breakBitstreamIteration = false;
        int bstatus = -10;
        InputStream inputstream = null;
        try {
            inputstream = bitstream.retrieve();
        } catch (IOException ioe) {
            bstatus = FILE_NOT_FOUND;
        }

        if (inputstream != null) {
            logDebugMessage("Scanning " + bitstream.getName() + " . . . ");

            bstatus = scan(bitstream, inputstream, itemHandle);
            inputstream.close();

            if (bstatus == Curator.CURATE_ERROR) {
                // no point going further - set result and error out
                setResult(SCAN_FAIL_MESSAGE);
                status = bstatus;
                breakBitstreamIteration = true;
            }
            if (isFailfast() && bstatus == Curator.CURATE_FAIL) {
                status = bstatus;
                breakBitstreamIteration = true;
            } else if (bstatus == Curator.CURATE_FAIL &&
                    status == Curator.CURATE_SUCCESS) {
                status = bstatus;
            }
        }

        boolean inWorkflow = itemHandle.equals(NEW_ITEM_HANDLE);
        postScanOperations(bitstream, inWorkflow, bstatus);
        return breakBitstreamIteration;
    }

    private List<PostScanOperation> getPostScanOperations() {
        PostScanOperation[] array = {
                new UpdateMetadata(),
                new RemoveAllPolicies()
        };
        return Arrays.asList(array);
    }

    protected void postScanOperations(Bitstream bitstream, boolean inWorkflow, int status) {
        List<PostScanOperation> processors = getPostScanOperations();

        Context context = this.context;
        boolean abortContext = false;
        if (context == null) {
            try {
                context = new Context();
                abortContext = true;
            } catch (SQLException | RuntimeException e) {
                log.error("Could not update bitstream after the virus scan " + bitstream.getID(), e);
            } finally {
                if (abortContext) {
                    context.abort();
                }
            }
        }

        if (context != null) {
            context.turnOffAuthorisationSystem();
            for (PostScanOperation processor : processors) {
                if (processor.isApplicable(inWorkflow, status)) {
                    processor.process(bitstream, context, status);
                }
            }

            try {
                if (commit) {
                    bitstream.update();
                    context.complete();
                }
                context.restoreAuthSystemState();
            } catch (SQLException | AuthorizeException | RuntimeException e) {
                log.error("Could not update bitstream after the virus scan " + bitstream.getID(), e);
            } finally {
                if (abortContext) {
                    context.abort();
                }
            }
        }
    }

    protected interface PostScanOperation {
        boolean isApplicable(boolean inWorkflow, int status);

        void process(Bitstream bitstream, Context context, int status);
    }

    public static class RemoveAllPolicies implements PostScanOperation {
        @Override
        public boolean isApplicable(boolean inWorkflow, int status) {
            return status == Curator.CURATE_FAIL && !inWorkflow;
        }

        @Override
        public void process(Bitstream bitstream, Context context, int status) {
            try {
                AuthorizeManager.removeAllPolicies(context, bitstream);
            } catch (SQLException e) {
                throw new RuntimeException("Could not remove the policies");
            }
        }
    }

    public static class UpdateMetadata implements PostScanOperation {

        protected Map<Integer, String> results;

        public UpdateMetadata() {
            this.results = new HashMap<>();
            results.put(Curator.CURATE_SUCCESS, "CLEAN");
            results.put(Curator.CURATE_FAIL, "VIRUS");
            results.put(FILE_NOT_FOUND, "FILE NOT FOUND");
            results.put(Curator.CURATE_ERROR, "ERROR");
        }

        @Override
        public boolean isApplicable(boolean inWorkflow, int status) {
            return true;
        }

        @Override
        public void process(Bitstream bitstream, Context context, int status) {
            MetadataUtils.updateVirusCheckDateField(bitstream);

            MetadataUtils.clearMetadata(bitstream, MetadataUtils.virusCheckResultField);
            String result = getResult(status);
            MetadataUtils.addMetadata(bitstream, MetadataUtils.virusCheckResultField, result);
        }

        private String getResult(int status) {
            return results.get(status);
        }
    }


    /** openSession
     *
     * This method opens a session.
     */

    private void openSession() throws IOException {
        socket = new Socket();
        try {
            logDebugMessage("Connecting to " + getHost() + ":" + getPort());
            socket.connect(new InetSocketAddress(getHost(), getPort()));
        } catch (IOException e) {
            log.error("Failed to connect to clamd . . .", e);
            throw (e);
        }
        try {
            socket.setSoTimeout(getTimeout());
        } catch (SocketException e) {
            log.error("Could not set socket timeout . . .  " + getTimeout() + "ms", e);
            throw (new IOException(e));
        }
        try {
            dataOutputStream = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            log.error("Failed to open OutputStream . . . ", e);
            throw (e);
        }

        try {
            dataOutputStream.write(IDSESSION);
        } catch (IOException e) {
            log.error("Error initiating session with IDSESSION command . . . ", e);
            throw (e);
        }
    }

    /** closeSession
     *
     * Close the IDSESSION in CLAMD
     *
     *
     */
    private void closeSession() {
        if (dataOutputStream != null) {
            try {
                dataOutputStream.write(END);
            } catch (IOException e) {
                log.error("Exception closing dataOutputStream", e);
            }
        }
        try {
            logDebugMessage("Closing the socket for ClamAv daemon . . . ");
            socket.close();
        } catch (IOException e) {
            log.error("Exception closing socket", e);
        }
    }

    /** scan
     *
     * Issue the INSTREAM command and return the response to
     * and from the clamav daemon
     *
     * @param the bitstream for reporting results
     * @param the InputStream to read
     * @param the item handle for reporting results
     * @return a ScanResult representing the server response
     * @throws IOException
     */
    final static byte[] buffer = new byte[DEFAULT_CHUNK_SIZE];
    ;

    private int scan(Bitstream bitstream, InputStream inputstream, String itemHandle) {
        try {
            dataOutputStream.write(INSTREAM);
        } catch (IOException e) {
            log.error("Error writing INSTREAM command . . .");
            return Curator.CURATE_ERROR;
        }
        int read = DEFAULT_CHUNK_SIZE;
        while (read == DEFAULT_CHUNK_SIZE) {
            try {
                read = inputstream.read(buffer);
            } catch (IOException e) {
                log.error("Failed attempting to read the InputStream . . . ");
                return Curator.CURATE_ERROR;
            }
            if (read == -1) {
                break;
            }
            try {
                dataOutputStream.writeInt(read);
                dataOutputStream.write(buffer, 0, read);
            } catch (IOException e) {
                log.error("Could not write to the socket . . . ");
                return Curator.CURATE_ERROR;
            }
        }
        try {
            dataOutputStream.writeInt(0);
            dataOutputStream.flush();
        } catch (IOException e) {
            log.error("Error writing zero-length chunk to socket");
            return Curator.CURATE_ERROR;
        }
        try {
            read = socket.getInputStream().read(buffer);

        } catch (IOException e) {
            log.error("Error reading result from socket");
            return Curator.CURATE_ERROR;
        }

        if (read > 0) {
            String response = new String(buffer, 0, read);
            logDebugMessage("Response: " + response);
            if (response.indexOf("FOUND") != -1) {
                String itemMsg = "item - " + itemHandle + ": ";
                String bsMsg = "bitstream - " + bitstream.getName() +
                        ": SequenceId - " + bitstream.getSequenceID() + ": infected";
                report(itemMsg + bsMsg);
                results.add(bsMsg);
                return Curator.CURATE_FAIL;
            } else {
                return Curator.CURATE_SUCCESS;
            }
        }
        return Curator.CURATE_ERROR;
    }

    private void formatResults(String itemHandle) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Item: ").append(itemHandle).append(" ");
        if (status == Curator.CURATE_FAIL) {
            sb.append(INFECTED_MESSAGE);
            int count = 0;
            for (String scanresult : results) {
                sb.append("\n").append(scanresult).append("\n");
                count++;
            }
            sb.append(count).append(" virus(es) found. ")
                    .append(" failfast: ").append(isFailfast());
        } else {
            sb.append(CLEAN_MESSAGE);
        }
        if (curator != null) {
            setResult(sb.toString());
        }
    }

    private static String getItemHandle(Item item) {
        String handle = item.getHandle();
        return (handle != null) ? handle : NEW_ITEM_HANDLE;
    }


    private void logDebugMessage(String message) {
        if (log.isDebugEnabled()) {
            log.debug(message);
        }
    }

    @Override
    protected void setResult(String result) {
        if(curator!=null) {
            super.setResult(result);
        }
    }

    @Override
    protected void report(String message)
    {
        if(curator!=null) {
            super.report(message);
        }

        log.warn(message);
    }
}
