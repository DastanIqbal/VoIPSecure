package local.security;

import java.io.InputStream;
import java.io.OutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

/** Classe che esegue la cifratura dei canali audio. */
public class CipherAudio {
	
	/** Impossibile istanziare oggetti di tipo CipherAudio. */
	private CipherAudio() {
	}
	/** Cifra l'input da periferica audio. */
	public static CipherInputStream send(InputStream audio_in_vs_end,SecretKeySpec SKey) {
		CipherInputStream cis=null;
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, SKey);			
			cis = new CipherInputStream(audio_in_vs_end, c);			
			
		} catch (Exception e) {
			System.out.println(e);
		}
		return cis;
	}
	/** Decifra i dati che saranno l'uotput della periferica audio. */
	public static CipherOutputStream receive(OutputStream audio_end_vs_out, SecretKeySpec SKey) {
		CipherOutputStream cos=null;
		try {
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.DECRYPT_MODE,SKey);
			cos = new CipherOutputStream(audio_end_vs_out, c);
		} catch (Exception e) {
			System.out.println(e);
		}
		return cos;
	}

}