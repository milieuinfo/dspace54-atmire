#!/bin/bash -e

source /etc/cumuli/*.bootstrap

export PATH="${tomcat_dir}/apache-ant/bin:$PATH"

export node_num=$(uname -n | awk 'BEGIN {FS="-"} {print $NF}')

# Replace variables
sed -i \
    -e "s/%vlan%/${vlan}/g" \
    -e "s/%ZUIL%/${ZUIL}/g" \
    -e "s/%zuil%/${zuil}/g" \
    -e "s/%ZUILURL%/${ZUILURL}/g" \
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
    -e "s/%dspace.archief.name%/${archief_name}/g" \
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
sed -i -e "s|%dspace.rest.url%|/${archief_name}/rest|g" ${tomcat_apps_dir}/dspace/webapps/rest/swagger-ui-2.1.0/dist/index.html
sed -i -e "s|%dspace.rest.url%|${archief_name}/rest|g" ${tomcat_apps_dir}/dspace/webapps/rest/api-docs/strapdown.html
sed -i -e "s|%dspace.rest.url%|/${archief_name}/rest/api|g" ${tomcat_apps_dir}/dspace/webapps/rest/api-docs/swagger.json

# Replacing dspace.dir in dspace.cfg file.
# The Ant script won't run properly as it loads this property through regexp property filtering.
sed -i -e "s|^\(dspace.dir[[:blank:]]*=[[:blank:]]*\).*$|\1${tomcat_apps_dir}/dspace|g" ${tomcat_apps_dir}/dspace/config/dspace.cfg
sed -i -e "s|^\(assetstore.dir[[:blank:]]*=[[:blank:]]*\).*$|\1${tomcat_apps_dir}/dspace/assetstore|g" ${tomcat_apps_dir}/dspace/config/dspace.cfg

sed -i -e "s/%google_analytics_key%/${info_gaId}/g" ${tomcat_apps_dir}/dspace/config/dspace.cfg

sed -i -e "s|\${sword.url}|${sword_url}|g" ${tomcat_apps_dir}/dspace/config/modules/swordv2-server.cfg

sed -i \
    -e "s|\${solr.server}|http://localhost:8080/${archief_name}/solr|g" \
    ${tomcat_apps_dir}/dspace/config/modules/oai.cfg \
    ${tomcat_apps_dir}/dspace/config/modules/discovery.cfg \
    ${tomcat_apps_dir}/dspace/config/modules/solr-statistics.cfg

# Symlinks into tomcat
#ln -f -s ${tomcat_apps_dir}/jspui.xml ${tomcat_home_dir}/conf/Catalina/localhost/jspui.xml
#ln -f -s ${tomcat_apps_dir}/lni.xml ${tomcat_home_dir}/conf/Catalina/localhost/lni.xml
#ln -f -s ${tomcat_apps_dir}/sword.xml ${tomcat_home_dir}/conf/Catalina/localhost/sword.xml
#ln -f -s ${tomcat_apps_dir}/oai.xml ${tomcat_home_dir}/conf/Catalina/localhost/oai.xml
ln -f -s ${tomcat_apps_dir}/rest.xml ${tomcat_home_dir}/conf/Catalina/localhost/${archief_name}#rest.xml
ln -f -s ${tomcat_apps_dir}/solr.xml ${tomcat_home_dir}/conf/Catalina/localhost/${archief_name}#solr.xml
ln -f -s ${tomcat_apps_dir}/swordv2.xml ${tomcat_home_dir}/conf/Catalina/localhost/${archief_name}#swordv2.xml
ln -f -s ${tomcat_apps_dir}/xmlui.xml ${tomcat_home_dir}/conf/Catalina/localhost/${archief_name}#xmlui.xml

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

cat << EOF > /etc/clamav/clamd.conf
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
LogSyslog true
LogRotate true
LogFacility LOG_LOCAL6
LogClean false
LogVerbose false
DatabaseDirectory /var/lib/clamav
OfficialDatabaseOnly false
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
CrossFilesystems true
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
PCREMatchLimit 10000
PCRERecMatchLimit 5000
PCREMaxFileSize 25M
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
EOF




cat << EOF > /etc/clamav/freshclam.conf
DatabaseOwner clamav
UpdateLogFile /var/log/clamav/freshclam.log
LogVerbose false
LogSyslog false
LogFacility LOG_LOCAL6
LogFileMaxSize 0
LogRotate true
LogTime true
Foreground false
Debug false
MaxAttempts 5
DatabaseDirectory /var/lib/clamav
DNSDatabaseInfo current.cvd.clamav.net
AllowSupplementaryGroups false
ConnectTimeout 30
ReceiveTimeout 30
TestDatabases yes
ScriptedUpdates yes
CompressLocalDatabase no
SafeBrowsing false
Bytecode true
NotifyClamd /etc/clamav/clamd.conf
# Check for new database 24 times a day
Checks 24
DatabaseMirror db.be.clamav.net
DatabaseMirror database.clamav.net
HTTPProxyServer ${http_proxy_host}
HTTPProxyPort ${http_proxy_port}

EOF

chmod 0700 /etc/clamav/freshclam.conf
chown root:root /etc/clamav/freshclam.conf

cat << EOF > /etc/systemd/system/clamav-daemon.socket.d/extend.conf
[Socket]
ListenStream=
SocketUser=clamav
ListenStream=127.0.0.1:3310
EOF
systemctl stop clamav-freshclam.service

freshclam

echo reload daemons
systemctl --system daemon-reload
systemctl start clamav-daemon.socket

echo start clamav
systemctl start clamav-daemon.service

echo start fleshclam
systemctl start clamav-freshclam.service
