package local.security;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.*;
//import java.security.InvalidKeyException;
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.NoSuchAlgorithmException;
//import java.security.NoSuchProviderException;
//import java.security.PrivateKey;
//import java.security.PublicKey;
//import java.security.SecureRandom;
//import java.security.Security;
//import java.security.SignatureException;
import java.util.Date;

import javax.crypto.*;
//import javax.crypto.SecretKey;
import javax.crypto.spec.*;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.zoolu.tools.Parser;


/**
 * Keys è la classe madre della coppia di chiavi RSA
 *  che andranno ad implementare la crittografia basata su PGP.
 *  <p>Ed anche la madre della della chiave simmetrica. 
 *
 */
public class Keys {
	
	private String identity;
	/** Password. */
	private String psw;
	/** Chiave simmetrica. */
	private SecretKey simmetricKey;
	/** Chiave simmetrica in formato Stringa. */
	private String simmetricKeyString;
	/** Specifica della chiave simmetrica usata per ricostruire la chiave simmetrica. */
	private SecretKeySpec skeySpec;
	/** Flag che indica se i file 
	 * <b>pub.asc</b>(chiave pubblica) e 
	 * <b>secret.asc</b>(chiave privata)
	 * sono presenti in ClassPath. */
	public boolean isPresent=false;
	/** Crea un oggetto Keys*/
	public Keys(String identity, String passPhrase,boolean isPresent) {
		this.identity=identity;
		this.psw=passPhrase;
		this.isPresent=isPresent;
		init();
	}
	/** Inizializza Keys. */
	public void init() {
		if(isPresent) return;
		genKeyPair(identity,psw);
	}
	/** Genera la coppia di chiavi RSA a 1024 bit. 
	 * <p> Tramite le quali utilizzando PGP(Pretty Good Privacy) esporta in ClassPath la chiave pubblica(pub.asc) e la chiave privata(secret.asc).
	 * */
	public void genKeyPair(String identity, String passPhrase) {
		try {
			Security.addProvider(new BouncyCastleProvider());

	        KeyPairGenerator    kpg = KeyPairGenerator.getInstance("RSA", "BC");
	        
	        kpg.initialize(1024);
	        
	        KeyPair kp = kpg.generateKeyPair();
	        
	        FileOutputStream    out1 = new FileOutputStream("secret.asc");
	        FileOutputStream    out2 = new FileOutputStream("pub.asc");
	        
	        exportKeyPair(out1, out2, kp.getPublic(), kp.getPrivate(), identity, passPhrase.toCharArray(), true);
	        
	        //POST sul KeyServer
	        if(!(pubKeyPOST())){
	        	System.exit(0);
	        }
		}catch(Exception e) {
		}
		
        
	}
	
	/** Crea la chiavi PGP e le esporta su file. */
	private void exportKeyPair(
	        OutputStream    secretOut,
	        OutputStream    publicOut,
	        PublicKey       publicKey,
	        PrivateKey      privateKey,
	        String          identity,
	        char[]          passPhrase,
	        boolean         armor)
	        throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException
	    {    
	        if (armor)
	        {
	            secretOut = new ArmoredOutputStream(secretOut);
	        }

	        PGPSecretKey    secretKey = new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION, PGPPublicKey.RSA_GENERAL, publicKey, privateKey, new Date(), identity, PGPEncryptedData.CAST5, passPhrase, null, null, new SecureRandom(), "BC");
	        
	        secretKey.encode(secretOut);
	        
	        secretOut.close();
	        
	        if (armor)
	        {
	            publicOut = new ArmoredOutputStream(publicOut);
	        }

	        PGPPublicKey    key = secretKey.getPublicKey();
	        
	        key.encode(publicOut);
	        
	        publicOut.close();
	    }
	
	/** POST della chiave pubblica(pub.asc) su un KeyServer (keyserver.ubuntu.com)*/
	private boolean pubKeyPOST() {
		if(pubKeyGET(identity)) {
			System.err.println("////////////////////////////////WARNING//////////////////////////////////");
			System.err.println("IS ALREADY ASSOCIATED A PUBLIC KEY TO THE FOLLOWING ID: "+identity);
			System.err.println("////////////////////////////////WARNING//////////////////////////////////");
			return false;
		}
		try {
		    // Construct data
			FileInputStream in = new FileInputStream("pub.asc");
			InputStreamReader isr = new InputStreamReader(in);
			BufferedReader br = new BufferedReader(isr);
			String aux = br.readLine();
			String buff=aux+"\r\n";
			while(aux!=null) {
				aux=br.readLine();
				buff+=aux+"\r\n";
				
			}
			br.close();
			
			String data = URLEncoder.encode("keytext", "UTF-8") + "=" + URLEncoder.encode(buff,"UTF-8"); 
		    // Send data
//		    URL url = new URL("http://pgpkeys.pca.dfn.de/pks/add");
//		    URL url = new URL("http://gpg-keyserver.de/pks/add");
//			URL url = new URL("http://keys.gnupg.net/pks/add");
		    URL url = new URL("http://keyserver.ubuntu.com:11371/pks/add");
		    URLConnection conn = url.openConnection();
		    conn.setDoOutput(true);
		    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		    wr.write(data);
		    wr.flush();

		    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		    String line;
		    while ((line = rd.readLine()) != null) {
//		        System.out.println(line);
		    }
		    wr.close();
		    rd.close();
//		    System.out.println(buff);
		    return true;
		}
		catch (UnsupportedEncodingException uee) {
			System.err.println(uee+"\n Encoding type not supported");
//			uee.printStackTrace();
//			System.exit(1);
			return false;
		}
		catch (MalformedURLException mue) {
			System.err.println(mue+"\n URL incorrect or no longer accessible");
//			mue.printStackTrace();
//			System.exit(1);
			return false;
		}
		catch(IOException ioe) {
			System.err.println(ioe);
//			ioe.printStackTrace();
//			System.exit(1);
			return false;
		}
	}
	
	/** GET della chiave pubblica(pub.asc) su un KeyServer (keyserver.ubuntu.com)*/
	public boolean pubKeyGET (String called) {
		  URL u;
		  InputStream is = null;
		  DataInputStream dis;
		  String s;
		  String buff="";;
		
		  try {
//		     u = new URL("http://pgpkeys.pca.dfn.de/pks/lookup?op=get&search="+called+);
//			 u = new URL("http://gpg-keyserver.de/pks/lookup?op=get&search="+called);
//			 u = new URL("http://keys.gnupg.net/pks/lookup?op=get&search="+called);
		     u = new URL("http://keyserver.ubuntu.com:11371/pks/lookup?op=get&search="+called);
		     is = u.openStream(); 
		     dis = new DataInputStream(new BufferedInputStream(is));
		     while ((s = dis.readLine()) != null) {
//		        System.out.println(s);
		        buff+=s+"\r\n";
		     }
		     savePubKeyRemote(buff);
		  } catch (MalformedURLException mue) {
		
		     System.err.println(mue+"\n Oops - URL wrong.");
//		     mue.printStackTrace();
		     System.exit(1);
		
		  } catch (IOException ioe) {
		
		     System.out.println("Oops - Not exist the public key associated with the user.");
//		     ioe.printStackTrace();
//		     System.exit(1);
		     return false;
		
		  }
		  return true;
		
		}
		
		/** Salva la chiave pubblica remota in pubRemote.asc. */
		private void savePubKeyRemote(String str) {
			try {
				Parser pars=new Parser(str);	
				pars.goTo("-----BEGIN PGP PUBLIC KEY BLOCK-----");
				FileOutputStream    out = new FileOutputStream("pubRemote.asc");
				PrintStream ps = new PrintStream(out);
				String buff=pars.getLine();
				while(!(buff.equals("-----END PGP PUBLIC KEY BLOCK-----"))) {
					ps.println(buff);
					buff=pars.getLine();
				}
				ps.println(buff);
			} catch (IOException ioe) {
				
			}
			
		}
		
	
	/** Restituisce la chiave simmetrica. */
	public SecretKey getSecretKey() {
		return simmetricKey;
	}
	/** Restituisce la specifica della chiave simmetrica. */
	public SecretKeySpec getSecretKeySpec() {
		return skeySpec;
	}
	/** Restituisce la password della coppia di chiavi PGP, 
	 * che corrisponde inoltre alla password dell'account SIP. 
	 * <p> <b>Da gestire con cautela</b>*/
	protected String getPsw() {
		return psw;
	}
	/** Restituisce l'identità della del proprietario delle chiavi PSP,
	 * che corrisponde all'ID dell'account SIP. */
	protected String getID() {
		return identity;
	}
	/** Genera la chiave simmetrica. */
	public String genSK(String called) {
		try {
			if(pubKeyGET(called)) {
				KeyGenerator kgen = KeyGenerator.getInstance("AES");
			    kgen.init(128,new SecureRandom());
			    simmetricKey = kgen.generateKey();
			    byte[] keyEnc = simmetricKey.getEncoded();
			    simmetricKeyString = ToolsKeys.byte2String(keyEnc);
			    skeySpec = new SecretKeySpec(keyEnc, "AES");
			    
			    FileOutputStream    out = new FileOutputStream("SK");
			    out.write(keyEnc);
			    out.close();
				return simmetricKeyString;
			}
			
			else {
				return null;
			}
		}
		catch (NoSuchAlgorithmException e) {
			System.err.println(e);
			e.printStackTrace();
            System.exit(1);
		}
		catch (FileNotFoundException e) { 
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}
		catch (IOException e) { 
			System.err.println(e);
			e.printStackTrace();
			System.exit(1); 
		}
		//mai eseguito
		return null;
	}	
	
	/** Ricostruisce la chiave simmetrica. */
	public void setSK(byte[] KKKs) throws Exception {
		try {
			
			//inizializzo il file su disco per decifrarlo
			FileOutputStream    out = new FileOutputStream("SK.bpg.bpg");
			out.write(KKKs);
			SignFile.sign("v","SK.bpg.bpg","pubRemote.asc",null);
			
			
			
			//decifro e salvo su SK.bpg
			simmetricKeyString = ToolsKeys.chiper("d","SK.bpg","secret.asc",psw);
			
			//prelevo la chiave simmetrica da file e la salvo in simmetricKey e skeySpec
			FileInputStream    in = new FileInputStream("SK");
    		byte [] keyEnc = new byte[ in.available() ]; 
    		
    		for (int i=0; i<keyEnc.length; i++) {
    			keyEnc[i] =(byte) in.read();
    		}  		
    		skeySpec = new SecretKeySpec(keyEnc, "AES");
		} 
		catch (FileNotFoundException e) {System.out.println(e+ "Error in Key.setSK()");}
		catch (IOException e) {}
	}
	/** firma il file passato come argomento e lo resituisce sottoforma di stringa
	 * @throws Exception */
	public String sign(String mode, String file, String key) throws Exception {
		return SignFile.sign(mode, file, key, psw);
	}	

}
