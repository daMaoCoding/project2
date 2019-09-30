FROM nimmis/java-centos:oracle-8-jdk
MAINTAINER tony@ddg.com
VOLUME /tmp
ADD target/pay-core-1.0.jar /app.jar
ENTRYPOINT ["/usr/bin/java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
	  && echo 'Asia/Shanghai' >/etc/timezone \