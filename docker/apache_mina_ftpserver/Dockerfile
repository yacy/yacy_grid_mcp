#FROM 8-jre-slim
FROM openjdk:13-jdk-alpine

# Build this with:
# docker build -t "yacygrid/apache_mina_ftpserver" .

# Run this with:
# docker run -v /tmp:apache-ftpserver-1.1.1/res/home/ -p 2121-2199:2121-2199 -d yacygrid/apache_mina_ftpserver

# Then connect with
# ftp -p localhost 2121
# and log in with admin:admin

RUN wget https://archive.apache.org/dist/mina/ftpserver/1.1.1/dist/apache-ftpserver-1.1.1.zip
RUN unzip -q apache-ftpserver-1.1.1.zip

# configure additional passive ports
RUN sed -i 's#<\/ssl>#<\/ssl><data-connection><passive ports="2122-2199"\/><\/data-connection>#g' apache-ftpserver-1.1.1/res/conf/ftpd-typical.xml

EXPOSE 2121-2199
CMD ["sh", "-c", "apache-ftpserver-1.1.1/bin/ftpd.sh res/conf/ftpd-typical.xml"]
