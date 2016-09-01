package org.dspace.app.itemexport;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 01 Sep 2016
 */
public class DateFileBasic extends DateFile {
    private static final String dateFileName = "last-date-export";
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final String charset = "UTF-8";
    private static Logger log = Logger.getLogger(DateFile.class);
    private final String dateFilePath;
    private final Date startDate;

    public DateFileBasic(String destDirName) {
        this.dateFilePath = destDirName + File.separator + dateFileName;
        this.startDate = new Date();
    }

    public Date getLastDate() {
        Date date = NO_DATE;

        File dateFile = new File(dateFilePath);
        if (dateFile.exists()) {
            Scanner scanner;
            try {
                scanner = new Scanner(dateFile, charset);
                String line = null;
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine();
                }

                try {
                    date = dateFormat.parse(line);
                } catch (ParseException e) {
                    log.warn("Could not parse a date out of the content of " + dateFilePath + "\n"
                            + "content: " + line);
                }

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return date;
    }

    public void writeDate() {
        File dateFile = new File(dateFilePath);
        try (PrintWriter printWriter = new PrintWriter(dateFile, "UTF-8")) {
            String dateFormatted = dateFormat.format(startDate);
            printWriter.write(dateFormatted);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
