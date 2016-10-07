#-----------------
# GLOBAL VARIABLES
#-----------------
# Full path of your local DSpace Installation (e.g. /home/dspace or /dspace or similar)
DSPACE_DIR=%tomcat_apps_dir%/dspace

# Shell to use
SHELL=/bin/bash

# Set JAVA_OPTS with defaults for DSpace Cron Jobs.
# Only provides 512MB of memory by default (which should be enough for most sites).
JAVA_OPTS="-Xmx2048M -Xms128M -XX:MaxPermSize=512M -Dfile.encoding=UTF-8"

#--------------
# BIDAILY TASKS (Recommended to be run multiple times per day, if possible)
#--------------

# Running the filter-media job regularly ensures that thumbnails are generated for newly add image/PDF
# files and also ensures full text search is available for newly added PDF/Word/PPT/HTML documents
0 4,12 * * * /usr/bin/flock -w 60 ~/cron-filter-media.lock $DSPACE_DIR/bin/dspace filter-media

# Update the OAI-PMH index with the newest content (and re-optimize that index) at twice every day
# NOTE: ONLY NECESSARY IF YOU ARE RUNNING OAI-PMH
# (This ensures new content is available via OAI-PMH and ensures the OAI-PMH index is optimized for better performance)
0 5,13 * * * /usr/bin/flock -w 60 ~/cron-oai.lock $DSPACE_DIR/bin/dspace oai import -o

#----------------
# DAILY TASKS
#----------------

# Create backups of SOLR, database and assest store data
0 23 * * * /usr/bin/flock -w 60 ~/cron-solr-backup.lock $DSPACE_DIR/bin/backup_solr.sh /opt/tomcat/data/backup/solr
#0 1 * * * /usr/bin/flock -w 60 ~/cron-db-backup.lock $DSPACE_DIR/bin/db-backup.sh -> DONE BY ACD
0 2 * * * /usr/bin/flock -w 60 ~/cron-assest-store-backup.lock $DSPACE_DIR/bin/backup_assetstore.sh /opt/tomcat/data/backup/assetstore

# Create an incremental export starting at 3 am every day
0 3 * * 5 /usr/bin/flock -w 60 ~/cron-export.lock $DSPACE_DIR/bin/dspace export -g -m -c -t COLLECTION -i acd/2 -d /opt/tomcat/data/export/IMJV
0 5 * * 5 /usr/bin/flock -w 60 ~/cron-export.lock $DSPACE_DIR/bin/dspace export -g -m -c -t COLLECTION -i acd/4 -d /opt/tomcat/data/export/DBA
0 7 * * 5 /usr/bin/flock -w 60 ~/cron-export.lock $DSPACE_DIR/bin/dspace export -g -m -c -t COLLECTION -i acd/5 -d /opt/tomcat/data/export/OMV

# Run any Curation Tasks queued from the Admin UI at 22:00 every day
# (Ensures that any curation task that an administrator "queued" from the Admin UI is executed
# asynchronously behind the scenes)
0 22 * * * /usr/bin/flock -w 60 ~/cron-curation.lock $DSPACE_DIR/bin/dspace curate -q admin_ui

# Send out subscription e-mails at 22:00 every day
# (This sends an email to any users who have "subscribed" to a Collection, notifying them of newly added content.)
0 22 * * * /usr/bin/flock -w 60 ~/cron-subscriptions.lock $DSPACE_DIR/bin/dspace sub-daily

#----------------
# WEEKLY TASKS
#----------------

# Re-Optimize the Discovery, Authority and Statistics indexes once a week on Saturday
# (This ensures that the Solr Index is re-optimized for better performance)
0 18 * * 6 /usr/bin/flock -w 60 ~/cron-discovery.lock $DSPACE_DIR/bin/dspace index-discovery -o
0 20 * * 6 /usr/bin/flock -w 60 ~/cron-authority.lock $DSPACE_DIR/bin/dspace index-authority -o
0 22 * * 6 /usr/bin/flock -w 60 ~/cron-statistics.lock $DSPACE_DIR/bin/dspace stats-util -o

# Run the checksum checker at 04:00 every Sunday
0 4 * * 0 /usr/bin/flock -w 60 ~/cron-checksum.lock $DSPACE_DIR/bin/dspace checker -d 4h -p

# Mail the results of the checksum checker (see above) to the configured "mail.admin" at 08:00 every Sunday.
# (This ensures the system administrator is notified whether any checksums were found to be different.)
0 9 * * 0 /usr/bin/flock -w 60 ~/cron-checksum.lock $DSPACE_DIR/bin/dspace checker-emailer

# Run the virus scanner at 04:00 every Saturday
0 4 * * 6 /usr/bin/flock -w 60 ~/cron-virusscan.lock $DSPACE_DIR/bin/dspace check-bitstreams -d 4h -p vscan

#----------------
# MONTHLY TASKS
#----------------
# Permanently delete any bitstreams flagged as "deleted" in DSpace, on the first of every month at 01:00
# (This ensures that any files which were deleted from DSpace are actually removed from your local filesystem.
#  By default they are just marked as deleted, but are not removed from the filesystem.)
# ATTENTION: ACD wants to keep all bitsreams, hence this job should not run automatically
#0 1 1 * * /usr/bin/flock -w 60 ~/cron-cleanup.lock $DSPACE_DIR/bin/dspace cleanup
