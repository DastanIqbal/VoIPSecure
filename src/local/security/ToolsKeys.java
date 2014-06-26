package local.security;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.zoolu.tools.Parser;

/** Classe di appoggio per la classe Keys, che svolge le funzioni fondamentali per la crittografia. */
public class ToolsKeys {
	/** Non è possibile istanziare oggetti di tipo ToolsKeys.*/
	private ToolsKeys() {	
	}
	
	
	/** Cripta o Decripta un file passato come argomento. */
	public static String chiper(String mode, String file, String keyFile, String pass) throws Exception  {
		
        Security.addProvider(new BouncyCastleProvider());
        
        if (mode.equals("e"))
        {
            FileInputStream     keyIn = new FileInputStream(keyFile);
            FileOutputStream    out = new FileOutputStream(file + ".bpg");
            encryptFile(out, file, readPublicKey(keyIn), false, false);
            return take(file + ".bpg");
            
        }
        else if (mode.equals("d"))
        {
            FileInputStream    in = new FileInputStream(file);
            //keyFile sarà secret.asc
            FileInputStream    keyIn = new FileInputStream(keyFile);
            decryptFile(in, keyIn, pass.toCharArray(), new File(file).getName() );
            
            return take(file);
        }
        else
        {
            System.err.println("usage: KeyBasedLargeFileProcessor -d|-e [-a|ai] file [secretKeyFile passPhrase|pubKeyFile]");
            return "";
        }
    }
	/** Resrituisce una stringa che rappresenta i byte di un file. */
	public static String take(String strIn) throws Exception {
		FileInputStream    in = new FileInputStream(strIn);
		byte [] b = new byte[ in.available() ]; 
		
		for (int i=0; i<b.length; i++) {
		   b[i] =(byte) in.read();
		}
//		int len = b.length;
		String x=byte2String(b);
        return x;
	}
	
	/** Cifra il file <i>filename</i> tramite chiave pubblica <i>encKey</i>, salvandolo sul file <i>out</i>*/
	private static void encryptFile(
	        OutputStream    out,
	        String          fileName,
	        PGPPublicKey    encKey,
	        boolean         armor,
	        boolean         withIntegrityCheck)
	        throws IOException, NoSuchProviderException
	    {    
	        if (armor)
	        {
	            out = new ArmoredOutputStream(out);
	        }
	        
	        try
	        {    
	            PGPEncryptedDataGenerator   cPk = new PGPEncryptedDataGenerator(PGPEncryptedData.CAST5, withIntegrityCheck, new SecureRandom(), "BC");
	                
	            cPk.addMethod(encKey);
	            
	            OutputStream                cOut = cPk.open(out, new byte[1 << 16]);
	            
	            PGPCompressedDataGenerator  comData = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
	            
	                                                                    
	            PGPUtil.writeFileToLiteralData(comData.open(cOut), PGPLiteralData.BINARY, new File(fileName), new byte[1 << 16]);
	            
	            comData.close();
	            
	            cOut.close();

	            out.close();
	        }
	        catch (PGPException e)
	        {	
	            System.err.println(e+"\nOops - Public Key isn't compatible with this software. It's impossible to call in secure mode. ");
//	            e.printStackTrace();
	            System.exit(1);
	        }
	    }
	
	/** Decifra il file <i>in</i> tramite la chiave privata e la password, salvando il file decifrato in <i>defaultFileName</i> */
	private static void decryptFile(
	        InputStream in,
	        InputStream keyIn,
	        char[]      passwd,
	        String      defaultFileName)
	        throws Exception
	    {    
	        in = PGPUtil.getDecoderStream(in);
	        
	        try
	        {
	            PGPObjectFactory        pgpF = new PGPObjectFactory(in);
	            PGPEncryptedDataList    enc;

	            Object                  o = pgpF.nextObject();
	            //
	            // the first object might be a PGP marker packet.
	            //
	            if (o instanceof PGPEncryptedDataList)
	            {
	                enc = (PGPEncryptedDataList)o;
	            }
	            else
	            {
	                enc = (PGPEncryptedDataList)pgpF.nextObject();
	            }
	            
	            //
	            // find the secret key
	            //
	            Iterator                    it = enc.getEncryptedDataObjects();
	            PGPPrivateKey               sKey = null;
	            PGPPublicKeyEncryptedData   pbe = null;
	            PGPSecretKeyRingCollection  pgpSec = new PGPSecretKeyRingCollection(
	                PGPUtil.getDecoderStream(keyIn));                                                                 
	            
	            while (sKey == null && it.hasNext())
	            {
	                pbe = (PGPPublicKeyEncryptedData)it.next();
	                
	                sKey = findSecretKey(pgpSec, pbe.getKeyID(), passwd);
	            }
	            
	            if (sKey == null)
	            {
	                throw new IllegalArgumentException("secret key for message not found.");
	            }
	            
	            InputStream         clear = pbe.getDataStream(sKey, "BC");
	            
	            PGPObjectFactory    plainFact = new PGPObjectFactory(clear);
	            
	            PGPCompressedData   cData = (PGPCompressedData)plainFact.nextObject();
	    
	            InputStream         compressedStream = new BufferedInputStream(cData.getDataStream());
	            PGPObjectFactory    pgpFact = new PGPObjectFactory(compressedStream);
	            
	            Object              message = pgpFact.nextObject();
	            
	            if (message instanceof PGPLiteralData)
	            {
	                PGPLiteralData       ld = (PGPLiteralData)message;

	                String               outFileName = ld.getFileName();
	                if (outFileName.length() == 0)
	                {
	                    outFileName = defaultFileName;
	                }
	                FileOutputStream     fOut = new FileOutputStream(outFileName);
	                BufferedOutputStream bOut = new BufferedOutputStream(fOut);
	                
	                InputStream    unc = ld.getInputStream();
	                int    ch;
	                
	                while ((ch = unc.read()) >= 0)
	                {
	                    bOut.write(ch);
	                }

	                bOut.close();
	            }
	            else if (message instanceof PGPOnePassSignatureList)
	            {
	                throw new PGPException("encrypted message contains a signed message - not literal data.");
	            }
	            else
	            {
	                throw new PGPException("message is not a simple encrypted file - type unknown.");
	            }
	        }
	        catch (PGPException e)
	        {
	            System.err.println(e);
	            if (e.getUnderlyingException() != null)
	            {
	                e.getUnderlyingException().printStackTrace();
	            }
	        }
	    }
	
	
	/** Cerca la secret key associata alla public key. */
	private static PGPPrivateKey findSecretKey(
	        PGPSecretKeyRingCollection  pgpSec,
	        long                        keyID,
	        char[]                      pass)
	        throws PGPException, NoSuchProviderException
	    {    
	        PGPSecretKey pgpSecKey = pgpSec.getSecretKey(keyID);
	        
	        if (pgpSecKey == null)
	        {
	            return null;
	        }
	        
	        return pgpSecKey.extractPrivateKey(pass, "BC");
	    }
	/** Restituisce la PGPPublicKey passatogli come argomento. */
	public static PGPPublicKey readPublicKey(
	        InputStream    in)
	        throws IOException, PGPException
	    {
	        in = PGPUtil.getDecoderStream(in);
	        
	        PGPPublicKeyRingCollection        pgpPub = new PGPPublicKeyRingCollection(in);

	        PGPPublicKey    key = null;
	        
	        Iterator rIt = pgpPub.getKeyRings();
	        
	        while (key == null && rIt.hasNext())
	        {
	            PGPPublicKeyRing    kRing = (PGPPublicKeyRing)rIt.next();    
	            Iterator            kIt = kRing.getPublicKeys();
	            
	            while (key == null && kIt.hasNext())
	            {
	                PGPPublicKey    k = (PGPPublicKey)kIt.next();
	                
	                if (k.isEncryptionKey())
	                {
	                    key = k;
	                }
	            }
	        }
	        
	        if (key == null)
	        {
	            throw new IllegalArgumentException("Can't find encryption key in key ring.");
	        }
	        in.close();
	        return key;
	    }

	/** Verifica se <i>ID</i> corrisponde con la chiave pubbclica <i>pubKey</i>. */
	public static boolean matching(String ID, FileInputStream pubKey) throws IOException, PGPException {
		PGPPublicKey p = ToolsKeys.readPublicKey(pubKey);
		Iterator i = p.getUserIDs();
		return i.next().equals(ID);
	}
	/** Trasforma un byte array in una Stringa. */
	public static String byte2String (byte[] arr) {
		String r="";
		int [] ii= new int[arr.length];
		for(int j=0; j<arr.length; j++) ii[j]=arr[j];
		for(int k=0;k<ii.length;k++) r+=" "+ii[k];
		return r.substring(1);
	}
	/** Trasforma una Stringa in un byte array. */
	public static byte[] string2Byte (String s,int len) {
		byte[] ris = new byte[len];
		int[] buff = new int[len];
		int lenStr=s.length();
		int j=0;
		for(int i=0;i<len;i++){
			String in="";
			for(;j+1<lenStr;j++) {
				if(s.substring(j,j+1).equals(" ")) {
					j++;
					break;
				}
				in+=s.substring(j,j+1);
			}
			if(in.equals("")) break;
			buff[i]=Integer.parseInt(in);
		}
	   
		//copy buff[] into a ris[];
		for(int k=0;k<ris.length;k++) ris[k]=((Integer)buff[k]).byteValue();
		return ris;
	}
	
	/** Estrapola la chiave(cifrata) incapsulata nel SDP.
	 * <p>La chiave è il valore dell'attributo <i>a=key-mgmt</i>.  */
	public static byte[] sdp2SK(String sdp){
		byte[] ris;
		Parser pars=new Parser(sdp);
		pars.goTo("a=key-mgmt");
		if(pars.startsWith("a=key-mgmt:SK")) {
			pars.goTo("SK");
			pars.skipString();
//			pars.skipString();
			int len = pars.getInt();
			pars.skipString();
			int init = pars.getPos();
			int end = pars.indexOf("]");
			String buff=(pars.getString(end-init)).substring(1);
			
			ris=string2Byte(buff,len);
			return ris;
		}
		else return null;
	}
	
	
	
	
}
