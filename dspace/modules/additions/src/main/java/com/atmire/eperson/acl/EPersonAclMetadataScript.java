package com.atmire.eperson.acl;

import com.atmire.eperson.acl.service.*;
import org.apache.commons.cli.*;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.content.*;
import org.dspace.core.*;
import org.dspace.eperson.*;
import org.dspace.utils.*;

/**
 * @author philip at atmire.com
 */
public class EPersonAclMetadataScript {

    private static final Logger log = Logger.getLogger(EPersonAclMetadataScript.class);

    private static String email;
    private static String fieldToUpdate;
    private static String fieldToRemove;
    private static String value;
    private static boolean listMetadata;

    public static void main(String[] args) {
        Context context = null;
        try {
            CommandLineParser parser = new PosixParser();

            Options options = CreateCommandLineOptions();

            CommandLine line = parser.parse(options, args);

            boolean exit = processArgs(line);

            if(exit){
                printHelp(options);
                System.exit(0);
            }

            context = new Context();
            context.turnOffAuthorisationSystem();

            EPersonAclMetadataService ePersonAclMetadataService = new DSpace().getServiceManager().getServicesByType(EPersonAclMetadataService.class).get(0);
            EPerson person = EPerson.findByEmail(context, email);

            if(person != null) {
                //update a metadata value
                if(StringUtils.isNotBlank(fieldToUpdate) && StringUtils.isNotBlank(value)){
                    if(!fieldContainsSchemaAndElement(ePersonAclMetadataService, fieldToUpdate)){
                        ePersonAclMetadataService.updateField(context, person, fieldToUpdate, value);
                        System.out.println("Field " + fieldToUpdate + " updated with value " + value);
                    }
                }

                if((StringUtils.isNotBlank(fieldToUpdate) && StringUtils.isBlank(value)) ||
                        (StringUtils.isBlank(fieldToUpdate) && StringUtils.isNotBlank(value))) {
                    System.out.println("Provide both a field (-u) and a value (-v) to update eperson metadata");
                }

                //remove all metadata values from a signle metadata field
                if(StringUtils.isNotBlank(fieldToRemove)) {
                    if(!fieldContainsSchemaAndElement(ePersonAclMetadataService, fieldToRemove)) {
                        ePersonAclMetadataService.removeField(context, person, fieldToRemove);
                        System.out.println("All metadata values removed from field " + fieldToRemove);
                    }
                }

                if(listMetadata){
                    printMetadata(person);
                }
            }
            else {
                System.out.println("No eperson found for email " + email);
            }


        } catch (Exception e) {
            log.error(e.getMessage(),e);
            System.out.println("an error occurred: " + e.getMessage());
        }
        finally {
            if(context != null && context.isValid()){
                context.abort();
            }
        }
    }

    private static void printMetadata(EPerson person){
        Metadatum[] metadata = person.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

        System.out.println("person " + person.getName() + " has " + metadata.length + " metadata values");

        for (Metadatum metadatum : metadata) {
            System.out.println(metadatum.getField() + " - " + metadatum.value);
        }
    }

    private static boolean fieldContainsSchemaAndElement(EPersonAclMetadataService ePersonAclMetadataService, String field) {
        boolean containsSchemaAndElement = ePersonAclMetadataService.fieldContainsSchemaAndElement(field);

        if(containsSchemaAndElement) {
            System.out.println("Field " + field + " needs to be specified without the schema and element indicator");
        }

        return containsSchemaAndElement;
    }

    private static Options CreateCommandLineOptions() {
        Options options = new Options();
        options.addOption("e", "email", true, "e-mail of the eperson");
        options.addOption("u", "update", true, "ACL metadata field to update");
        options.addOption("v", "value", true, "New value for the metadata field that is updated");
        options.addOption("r", "remove", true, "ACL metadata field to remove");
        options.addOption("l", "list", false, "List all metadata values of the eperson");
        options.addOption("h", "help", false, "help");

        return options;
    }

    private static boolean processArgs(CommandLine line) {
        if (line.hasOption("h")) {
            return true;
        }

        if (line.hasOption('e')) {
            email = line.getOptionValue("e");
        } else {
            return true;
        }

        if (line.hasOption('u')) {
            fieldToUpdate = line.getOptionValue("u");
        }

        if (line.hasOption('v')) {
            value = line.getOptionValue("v");
        }

        if (line.hasOption('r')) {
            fieldToRemove = line.getOptionValue("r");
        }

        listMetadata = line.hasOption('l');

        return false;
    }

    private static void printHelp(Options options) {
        HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("dsrun " + EPersonAclMetadataScript.class.getCanonicalName(), options);
    }
}
