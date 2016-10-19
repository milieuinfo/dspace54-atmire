#!/bin/sh

JAVA_OPTS="%jmx% %app_mem_params% -Dfile.encoding=UTF-8 -Dhttp.nonProxyHosts=\"%no_proxy_java%\" -Dhttp.proxyPort=%http_proxy_port% -Dhttp.proxyHost=%http_proxy_host% -Dhttps.proxyPort=%http_proxy_port% -Dhttps.proxyHost=%http_proxy_host% -Dhttp.proxyUser=%http_proxy_user% -Dhttp.proxyPassword=%http_proxy_password% %gc_logs% $JAVA_OPTS"
