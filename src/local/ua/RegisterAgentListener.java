package local.ua;


import org.zoolu.sip.address.NameAddress;


/** Listener di RegisterAgent */
public interface RegisterAgentListener
{
	/** Quando uno UA ha effettuato con successo la registrazione. */
   public void onUaRegistrationSuccess(RegisterAgent ra, NameAddress target, NameAddress contact, String result);

   /** Quando uno UA fallisce la registrazione. */
   public void onUaRegistrationFailure(RegisterAgent ra, NameAddress target, NameAddress contact, String result);

}
