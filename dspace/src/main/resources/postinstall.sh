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
    -e "s@%openam.public.server.url%@${openam_public_server_url}@g" \
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

mkdir -p ${tomcat_data_dir}/upload 2>/dev/null
chown -R tomcat:tomcat ${tomcat_data_dir}/upload

mkdir -p ${tomcat_data_dir}/imports 2>/dev/null
chown -R tomcat:tomcat ${tomcat_data_dir}/imports

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

echo "Toelaten om PDF's te converteren door ImageMagick"
sed -i 's/rights="none" pattern="PDF"/rights="read|write" pattern="PDF"/g' /etc/ImageMagick-6/policy.xml /etc/ImageMagick-6/policy.xml

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
DatabaseDirectory /var/lib/clamav
UpdateLogFile /var/log/clamav/freshclam.log
LogFileMaxSize 0
LogTime yes
LogVerbose false
LogRotate yes
DatabaseOwner clamav
PrivateMirror http://${artifactory_user}:${artifactory_password}@${artifactory_hostname}:${artifactory_port}/artifactory/Clamav
MaxAttempts 5
ScriptedUpdates no
Checks 24
NotifyClamd /etc/clamav/clamd.conf
EOF

/bin/chmod 0700 /etc/clamav/freshclam.conf
/bin/chown root:adm /etc/clamav/freshclam.conf

cat << EOF > /etc/clamav/clamd.conf
FixStaleSocket true
TCPSocket 3310
TCPAddr 127.0.0.1
User clamav
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
StreamMaxLength 500M
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
while  [ ! -f  /var/lib/clamav/main.c[vl]d ]
do
  ls -l /var/lib/clamav/
  sleep 5
done

echo "Sleep 20 om zeker te zijn dat alles goed is geladen"
sleep 20

/bin/systemctl start clamav-daemon.service
