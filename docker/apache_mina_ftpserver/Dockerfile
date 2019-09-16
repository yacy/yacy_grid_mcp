FROM openjdk:8-alpine AS builder
LABEL maintainer="Michael Peter Christen <mc@yacy.net>"

# Build this:
# docker build -t yacygrid/apache_mina_ftpserver:latest .

# Push the image:
# docker push yacygrid/apache_mina_ftpserver:latest

# Run:
# docker run -v /tmp:/apache-ftpserver-1.1.1/res/home/ -p 2121-2188:2121-2188 -d yacygrid/apache_mina_ftpserver

# Then connect with
# ftp -p localhost 2121
# and log in with admin:admin

RUN wget https://archive.apache.org/dist/mina/ftpserver/1.1.1/dist/apache-ftpserver-1.1.1.zip
RUN unzip -q apache-ftpserver-1.1.1.zip

# Second Stage
FROM openjdk:8-alpine

# Create Volume for persistence
VOLUME ["/apache-ftpserver-1.1.1/res/home/"]

# copy the required parts of the code
COPY --from=builder /apache-ftpserver-1.1.1 /apache-ftpserver-1.1.1

# configure additional passive ports
RUN sed -i 's#<\/ssl>#<\/ssl><data-connection><passive ports="2122-2188"\/><\/data-connection>#g' /apache-ftpserver-1.1.1/res/conf/ftpd-typical.xml

EXPOSE 2121-2188
CMD ["sh", "-c", "/apache-ftpserver-1.1.1/bin/ftpd.sh res/conf/ftpd-typical.xml"]
