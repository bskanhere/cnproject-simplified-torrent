
Team Members: Akanksha Miharia, Bhushan Kanhere, Mitul Mandaliya

Video Link: https://uflorida-my.sharepoint.com/personal/mitulmandaliya_ufl_edu/_layouts/15/onedrive.aspx

Common.cfg and PeerInfo.cfg are configuraiton files. 
PeerProcess is our main process from where the program execution begins. This creates connections to all its previous peers. 
It opens server socket to accept incoming connections from all peers after.
TCP connections are created between peers.
Handshake is performed to get peerID to associate with the socket.



Project is a maven project. Build project to generate jar. (mvn clean install)
Run project command - 
```java -jar cnproject-simplified-torrent.jar {peerID}```

