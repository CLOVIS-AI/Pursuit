FROM archlinux:latest

RUN pacman -Syuu --noconfirm jre-openjdk-headless

WORKDIR /opt/pursuit
COPY pursuit.tar /opt/pursuit/pursuit.tar
RUN tar -xf pursuit.tar && \
	rm pursuit.tar && \
	mv backend-*/* . && \
	rmdir backend-*

EXPOSE 8000

ENTRYPOINT [ "/opt/pursuit/bin/backend" ]
