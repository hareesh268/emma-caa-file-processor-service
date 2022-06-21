
#Start app along with Contrast scan based on environment variable WITH_CONTRAST
#WITH_CONTRAST=0 , to disable Contrast scan
#WITH_CONTRAST=1 , to enable Contrast scan pointing to dev contrast profile
#WITH_CONTRAST=2 , to enable Contrast scan pointing to qa contrast profile
#WITH_CONTRAST=3 , to enable Contrast scan pointing to prod contrast profile

if [ ! -z "${WITH_CONTRAST}" ]  &&  [ "$WITH_CONTRAST" -eq 1 ] ; then
    echo "> Contrast Scan : $WITH_CONTRAST"
	exec java -javaagent:./contrast-agent.jar -Dcontrast.config.path=./dev/contrast_security.yaml -Dserver.max-http-header-size=65536 -jar /app.jar	
elif [ ! -z "${WITH_CONTRAST}" ]  &&  [ "$WITH_CONTRAST" -eq 2 ] ; then
    echo "> Contrast Scan : $WITH_CONTRAST"
	exec java -javaagent:./contrast-agent.jar -Dcontrast.config.path=./qa/contrast_security.yaml -Dserver.max-http-header-size=65536 -jar /app.jar	
elif [ ! -z "${WITH_CONTRAST}" ]  &&  [ "$WITH_CONTRAST" -eq 3 ] ; then
    echo "> Contrast Scan : $WITH_CONTRAST"
	exec java -javaagent:./contrast-agent.jar -Dcontrast.config.path=./prod/contrast_security.yaml -Dserver.max-http-header-size=65536 -jar /app.jar	
else
    echo ": Contrast Scan : $WITH_CONTRAST"
	exec java -Dserver.max-http-header-size=65536 -jar /app.jar		
fi
