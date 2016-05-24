#!/bin/bash -e

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
    -e "s/%mailrelay_host%/${mailrelay_host}/g" \
    -e "s/%mailrelay_port%/${mailrelay_port}/g" \
    -e "s/%dspace.consumer.token%/${dspace_consumer_token}/g" \
    -e "s/%dspace.consumer.secret%/${dspace_consumer_secret}/g" \
    ${tomcat_apps_dir}/*.xml \
    ${tomcat_apps_dir}/cleanup.sh \
    ${tomcat_apps_dir}/setenv.sh \
    ${tomcat_apps_dir}/ansible.properties \
    ${tomcat_apps_dir}/dspace/config/modules/authentication-openam.cfg \
    ${tomcat_apps_dir}/dspace/bin/bulk/imjv-upload.sh

# Add the ansible substituted properties file to the head of dspace.cfg
cat ${tomcat_apps_dir}/ansible.properties | cat - ${tomcat_apps_dir}/dspace/config/dspace.cfg > ${tomcat_apps_dir}/dspace/config/dspace.cfg.tmp && mv -f ${tomcat_apps_dir}/dspace/config/dspace.cfg.tmp ${tomcat_apps_dir}/dspace/config/dspace.cfg

# Replacing dspace.dir in dspace.cfg file.
# The Ant script won't run properly as it loads this property through regexp property filtering.
sed -i -e "s|^\(dspace.dir[[:blank:]]*=[[:blank:]]*\).*$|\1${tomcat_apps_dir}/dspace|g" ${tomcat_apps_dir}/dspace/config/dspace.cfg

sed -i -e "s|\${sword.url}|http://${ZUIL}.milieuinfo.be:8080/archief/swordv2|g" ${tomcat_apps_dir}/dspace/config/modules/swordv2-server.cfg


# Symlinks into tomcat
#ln -f -s ${tomcat_apps_dir}/jspui.xml ${tomcat_home_dir}/conf/Catalina/localhost/jspui.xml
#ln -f -s ${tomcat_apps_dir}/lni.xml ${tomcat_home_dir}/conf/Catalina/localhost/lni.xml
#ln -f -s ${tomcat_apps_dir}/sword.xml ${tomcat_home_dir}/conf/Catalina/localhost/sword.xml
#ln -f -s ${tomcat_apps_dir}/oai.xml ${tomcat_home_dir}/conf/Catalina/localhost/oai.xml
ln -f -s ${tomcat_apps_dir}/rest.xml ${tomcat_home_dir}/conf/Catalina/localhost/archief#rest.xml
ln -f -s ${tomcat_apps_dir}/solr.xml ${tomcat_home_dir}/conf/Catalina/localhost/archief#solr.xml
ln -f -s ${tomcat_apps_dir}/swordv2.xml ${tomcat_home_dir}/conf/Catalina/localhost/archief#swordv2.xml
ln -f -s ${tomcat_apps_dir}/xmlui.xml ${tomcat_home_dir}/conf/Catalina/localhost/archief#xmlui.xml

ln -f -s ${tomcat_apps_dir}/setenv.sh ${tomcat_home_dir}/bin/setenv.sh


# Een trukje omdat dspace met zijn eigen installer komt en we zeker willen zijn dat alles goed werkt
mv ${tomcat_apps_dir}/dspace ${tomcat_apps_dir}/dspace_install


# Vermits er bij een ansible install alles wordt weg gesmeten moeten we er vanuit gaan dat er een clean install is
echo "Installeer toepassing"
cd ${tomcat_apps_dir}/dspace_install && ant -v init_installation init_configs install_code update_webapps clean_backups


# Als er nog geen assetstore is en we op node1 zitten wil het zeggen dat het een brand new installatie is en we zitten 
if [ ! -d "${tomcat_data_dir}/dspace/assetstore" && ${node_num} -eq 2  ]; then
    echo "Volledig nieuwe installatie"

    echo "Install van de db"
#    cd ${tomcat_apps_dir}/dspace && ant -v test_database load_registries

    echo "Creatie van data directories"
 #   mkdir "${tomcat_data_dir}/solr2"
 #   mkdir "${tomcat_data_dir}/assetstore"
 #   chown -R tomcat:tomcat ${tomcat_data_dir}/solr2
 #   chown -R tomcat:tomcat ${tomcat_data_dir}/assetstore
 

    # Create administrator
    # TODO uncomment this before releasing and deploying in oefen/productie
    #${tomcat_data_dir}/dspace/bin/dspace create-administrator -e 'dspace@milieuinfo.be' -f 'admin' -l 'dspace' -c 'en' -p 'DspacE'

    # (Create Communities, groups and policies)
    # TODO uncomment this before releasing and deploying in oefen/productie
    #${tomcat_apps_dir}/import-structure-policies.py -x -b ${tomcat_apps_dir}/dspace/bin/dspace -f ${tomcat_apps_dir}/dspace/config/community-tree.xml

fi

echo "Maken van symlinks naar de data folders"


cp -r ${tomcat_apps_dir}/dspace/solr/* ${tomcat_data_dir}/solr/

chown -R tomcat:tomcat ${tomcat_data_dir}/solr/

rm -rf ${tomcat_apps_dir}/dspace/solr

echo "Symlink voor solr source: ${tomcat_data_dir}/solr name: ${tomcat_apps_dir}/dspace/solr"
ln -s ${tomcat_data_dir}/solr ${tomcat_apps_dir}/dspace/solr

rm -rf ${tomcat_apps_dir}/dspace/assetstore

echo "Symlink voor assetstore source: ${tomcat_data_dir}/assetstore name: ${tomcat_apps_dir}/dspace/assetstore"
ln -s ${tomcat_data_dir}/assetstore ${tomcat_apps_dir}/dspace/assetstore

echo "Wissen van de install dir"
rm -rf ${tomcat_apps_dir}/dspace_install 

echo "Chown dir naar tomcat user : ${tomcat_apps_dir}"

chown -R tomcat:tomcat ${tomcat_apps_dir}



