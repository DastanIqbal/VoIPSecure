package local.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;

/**
 * A utility class that signs and verifies files.
 * <p>
 */
public class SignFile {
    /**
     * A simple routine that opens a key ring file and loads the first available key suitable for
     * signature generation.
     * 
     * @param in
     * @return
     * @throws IOException
     * @throws PGPException
     */
    private static PGPSecretKey readSecretKey(
        InputStream    in)
        throws IOException, PGPException
    {
        in = PGPUtil.getDecoderStream(in);
        
        PGPSecretKeyRingCollection        pgpSec = new PGPSecretKeyRingCollection(in);

       
        PGPSecretKey    key = null;
        
        //
        // iterate through the key rings.
        //
        Iterator rIt = pgpSec.getKeyRings();
        
        while (key == null && rIt.hasNext())
        {
            PGPSecretKeyRing    kRing = (PGPSecretKeyRing)rIt.next();    
            Iterator            kIt = kRing.getSecretKeys();
            
            while (key == null && kIt.hasNext())
            {
                PGPSecretKey    k = (PGPSecretKey)kIt.next();
                
                if (k.isSigningKey())
                {
                    key = k;
                }
            }
        }
        
        if (key == null)
        {
            throw new IllegalArgumentException("Can't find signing key in key ring.");
        }
        
        return key;
    }
    
    /**
     * verify the passed in file as being correctly signed.
     */
    private static void verifyFile(
        InputStream        in,
        InputStream        keyIn)
        throws Exception
    {
        in = PGPUtil.getDecoderStream(in);
        
        PGPObjectFactory            pgpFact = new PGPObjectFactory(in);

        PGPCompressedData           c1 = (PGPCompressedData)pgpFact.nextObject();

        pgpFact = new PGPObjectFactory(c1.getDataStream());
            
        PGPOnePassSignatureList     p1 = (PGPOnePassSignatureList)pgpFact.nextObject();
            
        PGPOnePassSignature         ops = p1.get(0);
            
        PGPLiteralData              p2 = (PGPLiteralData)pgpFact.nextObject();

        InputStream                 dIn = p2.getInputStream();
        int                         ch;
        PGPPublicKeyRingCollection  pgpRing = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(keyIn));

        PGPPublicKey                key = pgpRing.getPublicKey(ops.getKeyID());
        FileOutputStream            out = new FileOutputStream(p2.getFileName());

        ops.initVerify(key, "BC");
            
        while ((ch = dIn.read()) >= 0)
        {
            ops.update((byte)ch);
            out.write(ch);
        }

        out.close();
        
        PGPSignatureList            p3 = (PGPSignatureList)pgpFact.nextObject();

        if (ops.verify(p3.get(0)))
        {
            System.out.println("signature verified.");
        }
        else
        {
            System.out.println("signature verification failed.");
        }
    }

    /**
     * Generate an encapsulated signed file.
     * 
     * @param fileName
     * @param keyIn
     * @param out
     * @param pass
     * @param armor
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws PGPException
     * @throws SignatureException
     */
    private static void signFile(
        String          fileName,
        InputStream     keyIn,
        OutputStream    out,
        char[]          pass,
        boolean         armor)
        throws IOException, NoSuchAlgorithmException, NoSuchProviderException, PGPException, SignatureException
    {    
        if (armor)
        {
            out = new ArmoredOutputStream(out);
        }
        
        PGPSecretKey                pgpSec = readSecretKey(keyIn);
        PGPPrivateKey               pgpPrivKey = pgpSec.extractPrivateKey(pass, "BC");        
        PGPSignatureGenerator       sGen = new PGPSignatureGenerator(pgpSec.getPublicKey().getAlgorithm(), PGPUtil.SHA1, "BC");
        
        sGen.initSign(PGPSignature.BINARY_DOCUMENT, pgpPrivKey);
        
        Iterator    it = pgpSec.getPublicKey().getUserIDs();
        if (it.hasNext())
        {
            PGPSignatureSubpacketGenerator  spGen = new PGPSignatureSubpacketGenerator();
            
            spGen.setSignerUserID(false, (String)it.next());
            sGen.setHashedSubpackets(spGen.generate());
        }
        
        PGPCompressedDataGenerator  cGen = new PGPCompressedDataGenerator(
                                                                PGPCompressedData.ZLIB);
        
        BCPGOutputStream            bOut = new BCPGOutputStream(cGen.open(out));
        
        sGen.generateOnePassVersion(false).encode(bOut);
        
        File                        file = new File(fileName);
        PGPLiteralDataGenerator     lGen = new PGPLiteralDataGenerator();
        OutputStream                lOut = lGen.open(bOut, PGPLiteralData.BINARY, file);
        FileInputStream             fIn = new FileInputStream(file);
        int                         ch = 0;
        
        while ((ch = fIn.read()) >= 0)
        {
            lOut.write(ch);
            sGen.update((byte)ch);
        }
        
        lGen.close();
        
        sGen.generate().encode(bOut);
        
        cGen.close();
        
        out.close();
    }

    public static String sign(String mode, String file, String key, String psw) throws Exception   {
        Security.addProvider(new BouncyCastleProvider());

        if (mode.equals("s"))
        {

        	
                FileInputStream     keyIn = new FileInputStream(key);
                FileOutputStream    out = new FileOutputStream(file + ".bpg");
                
                signFile(file, keyIn, out, psw.toCharArray(), false);
                return ToolsKeys.take(file+".bpg");
        }
        else if (mode.equals("v"))
        {
            FileInputStream    in = new FileInputStream(file);
            FileInputStream    keyIn = new FileInputStream(key);
            
            verifyFile(in, keyIn);
            return ToolsKeys.take(file);
        }
        else
        {
            System.err.println("usage: SignedFileProcessor v|s file keyfile [passPhrase]");
            return null;
        }
    }
}