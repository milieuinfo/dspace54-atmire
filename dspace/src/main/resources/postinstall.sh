#!/bin/bash -e

source /etc/cumuli/*.bootstrap

export PATH="${tomcat_dir}/apache-ant/bin:$PATH"

export node_num=$(uname -n | awk 'BEGIN {FS="-"} {print $NF}')

# Replace variables
sed -i \
    -e "s/%vlan%/${vlan}/g" \
    -e "s/%ZUIL%/${ZUIL}/g" \
    -e "s/%zuil%/${zuil}/g" \
    -e "s/%rp_host%/${rp_host}/g" \
    -e "s@%tomcat_apps_dir%@${tomcat_apps_dir}@g" \
    -e "s@%tomcat_data_dir%@${tomcat_data_dir}@g" \
    -e "s@%tomcat_home_dir%@${tomcat_home_dir}@g" \
    -e "s/%db_username%/${db_username}/g" \
    -e "s/%db_password%/${db_password}/g" \
    -e "s/%db_name%/${db_name}/g" \
    -e "s/%db_port%/${db_port}/g" \
    -e "s/%db_host%/${db_host}/g" \
    -e "s/%db_max_active_rest%/${db_max_active_rest}/g" \
    -e "s/%db_max_idle_rest%/${db_max_idle_rest}/g" \
    -e "s/%db_min_idle_rest%/${db_min_idle_rest}/g" \
    -e "s/%db_max_wait_rest%/${db_max_wait_rest}/g" \
    -e "s/%db_max_active_sword%/${db_max_active_sword}/g" \
    -e "s/%db_max_idle_sword%/${db_max_idle_sword}/g" \
    -e "s/%db_min_idle_sword%/${db_min_idle_sword}/g" \
    -e "s/%db_max_wait_sword%/${db_max_wait_sword}/g" \
    -e "s/%db_max_active_xmlui%/${db_max_active_xmlui}/g" \
    -e "s/%db_max_idle_xmlui%/${db_max_idle_xmlui}/g" \
    -e "s/%db_min_idle_xmlui%/${db_min_idle_xmlui}/g" \
    -e "s/%db_max_wait_xmlui%/${db_max_wait_xmlui}/g" \
    -e "s/%mailrelay_host%/${mailrelay_host}/g" \
    -e "s/%mailrelay_port%/${mailrelay_port}/g" \
    -e "s/%mailrelay_disabled%/${mailrelay_disabled}/g" \
    -e "s/%mail_from_address%/${mail_from_address}/g" \
    -e "s/%mail_feedback_recipient%/${mail_feedback_recipient}/g" \
    -e "s/%mail_admin%/${mail_admin}/g" \
    -e "s/%dspace.consumer.token%/${dspace_consumer_token}/g" \
    -e "s/%dspace.consumer.secret%/${dspace_consumer_secret}/g" \
    -e "s/%openam.role.prefix%/${openam_role_prefix}/g" \
    -e "s/%openam.admin.role%/${openam_admin_role}/g" \
    -e "s@%openam.eid.token.goto.url%@${openam_eid_token_goto_url}@g" \
    -e "s@%openam.backend.server.url%@${openam_backend_server_url}@g" \
    -e "s@%jmx%@${jmx}@g" \
    -e "s@%app_mem_params%@${app_mem_params}@g" \
    -e "s@%gc_logs%@${gc_logs}@g" \
    -e "s@%http_proxy_host%@${http_proxy_host}@g" \
    -e "s@%http_proxy_port%@${http_proxy_port}@g" \
    -e "s@%http_proxy_user%@${http_proxy_user}@g" \
    -e "s@%http_proxy_password%@${http_proxy_password}@g" \
    -e "s@%no_proxy_java%@${no_proxy_java}@g" \
    ${tomcat_apps_dir}/*.xml \
    ${tomcat_apps_dir}/*.sh \
    ${tomcat_apps_dir}/ansible.properties \
    ${tomcat_apps_dir}/dspace/config/modules/authentication-openam.cfg


# Add the ansible substituted properties file to the head of dspace.cfg
cat ${tomcat_apps_dir}/ansible.properties | cat - ${tomcat_apps_dir}/dspace/config/dspace.cfg > ${tomcat_apps_dir}/dspace/config/dspace.cfg.tmp && mv -f ${tomcat_apps_dir}/dspace/config/dspace.cfg.tmp ${tomcat_apps_dir}/dspace/config/dspace.cfg

# Fixing some swagger stuff
sed -i -e "s|%dspace.rest.url%|/rest|g" ${tomcat_apps_dir}/dspace/webapps/rest/swagger-ui-2.1.0/dist/index.html
sed -i -e "s|%dspace.rest.url%|rest|g" ${tomcat_apps_dir}/dspace/webapps/rest/api-docs/strapdown.html
sed -i -e "s|%dspace.rest.url%|/rest/api|g" ${tomcat_apps_dir}/dspace/webapps/rest/api-docs/swagger.json

# Replacing dspace.dir in dspace.cfg file.
# The Ant script won't run properly as it loads this property through regexp property filtering.
sed -i -e "s|^\(dspace.dir[[:blank:]]*=[[:blank:]]*\).*$|\1${tomcat_apps_dir}/dspace|g" ${tomcat_apps_dir}/dspace/config/dspace.cfg
sed -i -e "s|^\(assetstore.dir[[:blank:]]*=[[:blank:]]*\).*$|\1${tomcat_apps_dir}/dspace/assetstore|g" ${tomcat_apps_dir}/dspace/config/dspace.cfg

sed -i -e "s/%google_analytics_key%/${info_gaId}/g" ${tomcat_apps_dir}/dspace/config/dspace.cfg

sed -i -e "s|\${sword.url}|${sword_url}|g" ${tomcat_apps_dir}/dspace/config/modules/swordv2-server.cfg

sed -i \
    -e "s|\${solr.server}|http://localhost:8080/solr|g" \
    ${tomcat_apps_dir}/dspace/config/modules/oai.cfg \
    ${tomcat_apps_dir}/dspace/config/modules/discovery.cfg \
    ${tomcat_apps_dir}/dspace/config/modules/solr-statistics.cfg

# Symlinks into tomcat
#ln -f -s ${tomcat_apps_dir}/jspui.xml ${tomcat_home_dir}/conf/Catalina/localhost/jspui.xml
#ln -f -s ${tomcat_apps_dir}/lni.xml ${tomcat_home_dir}/conf/Catalina/localhost/lni.xml
#ln -f -s ${tomcat_apps_dir}/sword.xml ${tomcat_home_dir}/conf/Catalina/localhost/sword.xml
#ln -f -s ${tomcat_apps_dir}/oai.xml ${tomcat_home_dir}/conf/Catalina/localhost/oai.xml
ln -f -s ${tomcat_apps_dir}/rest.xml ${tomcat_home_dir}/conf/Catalina/localhost/rest.xml
ln -f -s ${tomcat_apps_dir}/solr.xml ${tomcat_home_dir}/conf/Catalina/localhost/solr.xml
ln -f -s ${tomcat_apps_dir}/swordv2.xml ${tomcat_home_dir}/conf/Catalina/localhost/swordv2.xml
ln -f -s ${tomcat_apps_dir}/xmlui.xml ${tomcat_home_dir}/conf/Catalina/localhost/xmlui.xml

ln -f -s ${tomcat_apps_dir}/setenv.sh ${tomcat_home_dir}/bin/setenv.sh


# Een trukje omdat dspace met zijn eigen installer komt en we zeker willen zijn dat alles goed werkt
mv ${tomcat_apps_dir}/dspace ${tomcat_apps_dir}/dspace_install


# Vermits er bij een ansible install alles wordt weg gesmeten moeten we er vanuit gaan dat er een clean install is
echo "Installeer toepassing"
export ANT_OPTS="-Dfile.encoding=UTF-8"
cd ${tomcat_apps_dir}/dspace_install && ant -v init_installation init_configs install_code update_webapps clean_backups


echo "Maken van Data directories"
chown tomcat:tomcat ${tomcat_data_dir}

mkdir -p ${tomcat_data_dir}/solr 2>/dev/null


mkdir -p ${tomcat_data_dir}/GeoLite 2>/dev/null

mkdir -p ${tomcat_data_dir}/assetstore 2>/dev/null

# Installatie GeoLite
echo "Installatie GeoLite"
if [ -a ${tomcat_data_dir}/GeoLite/GeoLiteCity.dat ]; then
    echo "GeoLite al aanwezig niets te doen"
else
    echo "Installatie GeoLite db"
    gzip -c /tmp/GeoLiteCity.dat.gz > ${tomcat_data_dir}/GeoLite/GeoLiteCity.dat
    chown -R tomcat:tomcat ${tomcat_data_dir}/GeoLite
fi


# Installatie Solr
echo "Installatie Solr..."
if [ "$(ls -A ${tomcat_data_dir}/solr/)" ]; then
    echo "Solr al aanwezig, enkel configuratie updaten"
    cp -r ${tomcat_apps_dir}/dspace/solr/authority/conf ${tomcat_data_dir}/solr/authority/
    chown -R tomcat:tomcat ${tomcat_data_dir}/solr/authority/conf
    cp -r ${tomcat_apps_dir}/dspace/solr/oai/conf ${tomcat_data_dir}/solr/oai/
    chown -R tomcat:tomcat ${tomcat_data_dir}/solr/oai/conf
    cp -r ${tomcat_apps_dir}/dspace/solr/search/conf ${tomcat_data_dir}/solr/search/
    chown -R tomcat:tomcat ${tomcat_data_dir}/solr/search/conf
    cp -r ${tomcat_apps_dir}/dspace/solr/statistics/conf ${tomcat_data_dir}/solr/statistics/
    chown -R tomcat:tomcat ${tomcat_data_dir}/solr/statistics/conf
else
    echo "Nieuwe installatie Solr"
    cp -r ${tomcat_apps_dir}/dspace/solr/* ${tomcat_data_dir}/solr/
    chown -R tomcat:tomcat ${tomcat_data_dir}/solr/
fi


echo "Symlink voor solr source: ${tomcat_data_dir}/solr name: ${tomcat_apps_dir}/dspace/solr"
rm -rf ${tomcat_apps_dir}/dspace/solr
ln -s ${tomcat_data_dir}/solr ${tomcat_apps_dir}/dspace/solr

echo "Symlink voor assetstore source: ${tomcat_data_dir}/assetstore name: ${tomcat_apps_dir}/dspace/assetstore"
rm -rf ${tomcat_apps_dir}/dspace/assetstore
chown tomcat:tomcat ${tomcat_data_dir}/assetstore/
ln -s ${tomcat_data_dir}/assetstore ${tomcat_apps_dir}/dspace/assetstore

echo "Symlink maken naar /var/log/dspace"

mkdir -p /var/log/dspace 2>/dev/null

chown tomcat:tomcat /var/log/dspace

rm -rf ${tomcat_apps_dir}/dspace/log

ln -s /var/log/dspace ${tomcat_apps_dir}/dspace/log


echo "Wissen van de install dir"
rm -rf ${tomcat_apps_dir}/dspace_install 

echo "Chown dir naar tomcat user : ${tomcat_apps_dir}"

chown -R tomcat:tomcat ${tomcat_apps_dir}
chown -R tomcat:tomcat ${tomcat_apps_dir}/dspace


echo "Cleanup temp dir"
rm -rf /tmp/apache-ant-1.9.4-bin.zip
rm -rf /tmp/dspace-install-pkg.zip
rm -rf /tmp/GeoLiteCity.dat.gz


crontab -u tomcat ${tomcat_apps_dir}/crontab.sh

echo "Config ClamAV"
/bin/systemctl stop clamav-freshclam.service
/bin/systemctl stop clamav-daemon.service

mkdir -p /var/run/clamav

chown clamav:clamav /var/run/clamav

cat << EOF > /etc/clamav/freshclam.conf
##
## Please read the freshclam.conf(5) manual before editing this file.
##


# Path to the database directory.
# WARNING: It must match clamd.conf's directive!
# Default: hardcoded (depends on installation options)
DatabaseDirectory /var/lib/clamav

# Path to the log file (make sure it has proper permissions)
# Default: disabled
UpdateLogFile /var/log/clamav/freshclam.log

# Maximum size of the log file.
# Value of 0 disables the limit.
# You may use 'M' or 'm' for megabytes (1M = 1m = 1048576 bytes)
# and 'K' or 'k' for kilobytes (1K = 1k = 1024 bytes).
# in bytes just don't use modifiers. If LogFileMaxSize is enabled,
# log rotation (the LogRotate option) will always be enabled.
# Default: 1M
#LogFileMaxSize 2M
LogFileMaxSize 0

# Log time with each message.
# Default: no
LogTime yes

# Enable verbose logging.
# Default: no
LogVerbose yes

# Use system logger (can work together with UpdateLogFile).
# Default: no
#LogSyslog yes

# Specify the type of syslog messages - please refer to 'man syslog'
# for facility names.
# Default: LOG_LOCAL6
#LogFacility LOG_MAIL

# Enable log rotation. Always enabled when LogFileMaxSize is enabled.
# Default: no
LogRotate yes

# This option allows you to save the process identifier of the daemon
# Default: disabled
#PidFile /var/run/freshclam.pid

# By default when started freshclam drops privileges and switches to the
# "clamav" user. This directive allows you to change the database owner.
# Default: clamav (may depend on installation options)
DatabaseOwner clamav

# Initialize supplementary group access (freshclam must be started by root).
# Default: no
#AllowSupplementaryGroups yes

# Use DNS to verify virus database version. Freshclam uses DNS TXT records
# to verify database and software versions. With this directive you can change
# the database verification domain.
# WARNING: Do not touch it unless you're configuring freshclam to use your
# own database verification domain.
# Default: current.cvd.clamav.net
#DNSDatabaseInfo current.cvd.clamav.net

# Uncomment the following line and replace XY with your country
# code. See http://www.iana.org/cctld/cctld-whois.htm for the full list.
# You can use db.XY.ipv6.clamav.net for IPv6 connections.
#DatabaseMirror db.XY.clamav.net
DatabaseMirror db.be.clamav.net

# database.clamav.net is a round-robin record which points to our most
# reliable mirrors. It's used as a fall back in case db.XY.clamav.net is
# not working. DO NOT TOUCH the following line unless you know what you
# are doing.
DatabaseMirror database.clamav.net

# How many attempts to make before giving up.
# Default: 3 (per mirror)
MaxAttempts 5

# With this option you can control scripted updates. It's highly recommended
# to keep it enabled.
# Default: yes
ScriptedUpdates yes

# By default freshclam will keep the local databases (.cld) uncompressed to
# make their handling faster. With this option you can enable the compression;
# the change will take effect with the next database update.
# Default: no
#CompressLocalDatabase no

# With this option you can provide custom sources (http:// or file://) for
# database files. This option can be used multiple times.
# Default: no custom URLs
#DatabaseCustomURL http://myserver.com/mysigs.ndb
#DatabaseCustomURL file:///mnt/nfs/local.hdb

# This option allows you to easily point freshclam to private mirrors.
# If PrivateMirror is set, freshclam does not attempt to use DNS
# to determine whether its databases are out-of-date, instead it will
# use the If-Modified-Since request or directly check the headers of the
# remote database files. For each database, freshclam first attempts
# to download the CLD file. If that fails, it tries to download the
# CVD file. This option overrides DatabaseMirror, DNSDatabaseInfo
# and ScriptedUpdates. It can be used multiple times to provide
# fall-back mirrors.
# Default: disabled
#PrivateMirror mirror1.mynetwork.com
#PrivateMirror mirror2.mynetwork.com

# Number of database checks per day.
# Default: 12 (every two hours)
Checks 24

# Proxy settings
# Default: disabled
HTTPProxyServer ${http_proxy_host}
HTTPProxyPort ${http_proxy_port}

# If your servers are behind a firewall/proxy which applies User-Agent
# filtering you can use this option to force the use of a different
# User-Agent header.
# Default: clamav/version_number
#HTTPUserAgent SomeUserAgentIdString

# Use aaa.bbb.ccc.ddd as client address for downloading databases. Useful for
# multi-homed systems.
# Default: Use OS'es default outgoing IP address.
#LocalIPAddress aaa.bbb.ccc.ddd

# Send the RELOAD command to clamd.
# Default: no
NotifyClamd /etc/clamav/clamd.conf

# Run command after successful database update.
# Default: disabled
#OnUpdateExecute command

# Run command when database update process fails.
# Default: disabled
#OnErrorExecute command

# Run command when freshclam reports outdated version.
# In the command string %v will be replaced by the new version number.
# Default: disabled
#OnOutdatedExecute command

# Don't fork into background.
# Default: no
#Foreground yes

# Enable debug messages in libclamav.
# Default: no
#Debug yes

# Timeout in seconds when connecting to database server.
# Default: 30
#ConnectTimeout 60

# Timeout in seconds when reading from database server.
# Default: 30
#ReceiveTimeout 60

# With this option enabled, freshclam will attempt to load new
# databases into memory to make sure they are properly handled
# by libclamav before replacing the old ones.
# Default: yes
#TestDatabases yes

# When enabled freshclam will submit statistics to the ClamAV Project about
# the latest virus detections in your environment. The ClamAV maintainers
# will then use this data to determine what types of malware are the most
# detected in the field and in what geographic area they are.
# Freshclam will connect to clamd in order to get recent statistics.
# Default: no
#SubmitDetectionStats /path/to/clamd.conf

# Country of origin of malware/detection statistics (for statistical
# purposes only). The statistics collector at ClamAV.net will look up
# your IP address to determine the geographical origin of the malware
# reported by your installation. If this installation is mainly used to
# scan data which comes from a different location, please enable this
# option and enter a two-letter code (see http://www.iana.org/domains/root/db/)
# of the country of origin.
# Default: disabled
#DetectionStatsCountry country-code

# This option enables support for our "Personal Statistics" service.
# When this option is enabled, the information on malware detected by
# your clamd installation is made available to you through our website.
# To get your HostID, log on http://www.stats.clamav.net and add a new
# host to your host list. Once you have the HostID, uncomment this option
# and paste the HostID here. As soon as your freshclam starts submitting
# information to our stats collecting service, you will be able to view
# the statistics of this clamd installation by logging into
# http://www.stats.clamav.net with the same credentials you used to
# generate the HostID. For more information refer to:
# http://www.clamav.net/documentation.html#cctts
# This feature requires SubmitDetectionStats to be enabled.
# Default: disabled
#DetectionStatsHostID unique-id

# This option enables support for Google Safe Browsing. When activated for
# the first time, freshclam will download a new database file (safebrowsing.cvd)
# which will be automatically loaded by clamd and clamscan during the next
# reload, provided that the heuristic phishing detection is turned on. This
# database includes information about websites that may be phishing sites or
# possible sources of malware. When using this option, it's mandatory to run
# freshclam at least every 30 minutes.
# Freshclam uses the ClamAV's mirror infrastructure to distribute the
# database and its updates but all the contents are provided under Google's
# terms of use. See http://www.google.com/transparencyreport/safebrowsing
# and http://www.clamav.net/documentation.html#safebrowsing
# for more information.
# Default: disabled
#SafeBrowsing yes

# This option enables downloading of bytecode.cvd, which includes additional
# detection mechanisms and improvements to the ClamAV engine.
# Default: enabled
#Bytecode yes

# Download an additional 3rd party signature database distributed through
# the ClamAV mirrors.
# This option can be used multiple times.
#ExtraDatabase dbname1
#ExtraDatabase dbname2

EOF

/bin/chmod 0700 /etc/clamav/freshclam.conf
/bin/chown root:adm /etc/clamav/freshclam.conf

cat << EOF > /etc/clamav/clamd.conf
#Automatically Generated by clamav-base postinst
#To reconfigure clamd run #dpkg-reconfigure clamav-base
#Please read /usr/share/doc/clamav-base/README.Debian.gz for details
#LocalSocket /var/run/clamav/clamd.ctl
FixStaleSocket true
#LocalSocketGroup clamav
#LocalSocketMode 666
TCPSocket 3310
TCPAddr 127.0.0.1
# TemporaryDirectory is not set to its default /tmp here to make overriding
# the default with environment variables TMPDIR/TMP/TEMP possible
User clamav
AllowSupplementaryGroups false
ScanMail true
ScanArchive true
ArchiveBlockEncrypted false
MaxDirectoryRecursion 15
FollowDirectorySymlinks false
FollowFileSymlinks false
ReadTimeout 180
MaxThreads 12
MaxConnectionQueueLength 15
LogSyslog false
LogRotate true
LogFacility LOG_LOCAL6
LogClean false
LogVerbose false
PidFile /var/run/clamav/clamd.pid
DatabaseDirectory /var/lib/clamav
SelfCheck 3600
Foreground false
Debug false
ScanPE true
MaxEmbeddedPE 10M
ScanOLE2 true
ScanPDF true
ScanHTML true
MaxHTMLNormalize 10M
MaxHTMLNoTags 2M
MaxScriptNormalize 5M
MaxZipTypeRcg 1M
ScanSWF true
DetectBrokenExecutables false
ExitOnOOM false
LeaveTemporaryFiles false
AlgorithmicDetection true
ScanELF true
IdleTimeout 30
PhishingSignatures true
PhishingScanURLs true
PhishingAlwaysBlockSSLMismatch false
PhishingAlwaysBlockCloak false
PartitionIntersection false
DetectPUA false
ScanPartialMessages false
HeuristicScanPrecedence false
StructuredDataDetection false
CommandReadTimeout 5
SendBufTimeout 200
MaxQueue 100
ExtendedDetectionInfo true
OLE2BlockMacros false
ScanOnAccess false
AllowAllMatchScan true
ForceToDisk false
DisableCertCheck false
DisableCache false
MaxScanSize 100M
MaxFileSize 25M
MaxRecursion 16
MaxFiles 10000
MaxPartitions 50
MaxIconsPE 100
StatsEnabled false
StatsPEDisabled true
StatsHostID auto
StatsTimeout 10
StreamMaxLength 100M
LogFile /var/log/clamav/clamav.log
LogTime true
LogFileUnlock false
LogFileMaxSize 0
Bytecode true
BytecodeSecurity TrustSigned
BytecodeTimeout 60000
OfficialDatabaseOnly false
CrossFilesystems true

EOF

/bin/systemctl start clamav-freshclam.service

echo "Wachten op virus definities..."
## wait until the virus-definitions are available on the system before starting the clamav-daemon
while  [ ! -f  /var/lib/clamav/daily.c[vl]d ]
do
  sleep 5
done

/bin/systemctl start clamav-daemon.service
