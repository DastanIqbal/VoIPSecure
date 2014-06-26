VoIPSecure

==========


Voice over IP encrypted using SIP protocol standard
_____________________________________________________________________________________
VoipSecure -----
	|------- build.xml

	+--------- config ------ 
			|------- config.cfg
	+--------- lib ---------
			|------- bcpg.jar
			|------- bcprov.jar
			|------- sip.jar
	+--------- media ------- 
			| ------- off.wav
			| ------- on.wav
			| ------- ring.wav
	+--------- src
		+-------- local
			+-------- media -------- 
					|------- AudioClipPlayer.java
					|------- AudioClipPlayerListener.java
					|------- AudioInput.java
					|------- AudioOutput.java
					|------- AudioOutputStream.java
					|------- RtpStreamReceiver.java
					|------- RtpStreamSender.java

			+-------- net ---------- 
					|------- KeepAliveSip.java
					|------- KeepAliveUdp.java
					|------- RtpPacket.java
					|------- RtpSocket.java 

			+-------- security -----
					|------- CipherAudio.java
					|------- Keys.java
					|------- ToolsKeys.java
			+-------- ua -----------
					|------- JAudioLauncher.java
					|------- Main.java
					|------- MediaLauncher.java
					|------- RegisterAgent.java
					|------- RegisterAgentListener.java
					|------- UserAgent.java
					|------- UserAgentListener.java
					|------- UserAgentProfile.java
_____________________________________________________________________________________


__________________________________Requisiti Minimi___________________________________:
Account Sip attivo (raccomandato: http://www.iptel.org/ )
JDK_1.6 o JRE_1.6
ANT(building tool) , disponibile all'indirizzo : http://ant.apache.org/

____________________________________Building ________________________________________:
Target :
	prepare
	compile
	javadoc
	dist
	clean
	all

Eseguire da shell il comando "ant all", per mezzo del quale verrà creata una directory
 "dist", dentro cui sarà disponibile tutto il materiale necessario per l'uso e la
distribuzione del software, comprensiva di una dettagliata documentazione.


______________________________________Init___________________________________________:

SPOSTARSI DENTRO LA DIRECTORY "dist". 
Come primo passo FONDAMENTALE bisogna andare a configurare il file config.cfg(edit 
come file di testo) dentro la directory /config/. E' consigliato inserire solo le 
credenziali d'accesso al proprio account sip, tra i quali:
 host_addr,user,psw, ed indirizzo sip completo.
Lasciando pressochè invariati invariati gli altri campi.


______________________________________Exe____________________________________________:

digitare da shell il seguente comando: 
"java -classpath lib/sip.jar;lib/bcpg.jar;lib/bcprov.jar;lib/VoipSecure_0.0.1.jar local.ua.Main -f config/config.cfg"


______________________________________NOTE____________________________________________:

In fase di startup il software andrà a cercare nella propria root i certificati PGP, 
quindi la coppia di chiavi associata all'account SIP, qualora non la trovasse verrebbe 
creata ex-novo una nuova coppia e salvata nella directory principale("pub.asc" e 
"secret.asc"), inoltrando pub.asc al Key Server di riferimento.
Si consiglia pertanto di conservare con cautela una copia dei file pub.asc e secret.asc
 per usi futuri, visto che nel Key Server sarà permanente l'associazione 
pub.asc-->account SIP, e una nuova associazione per mezzo di questo software non è 
al momento possibile.