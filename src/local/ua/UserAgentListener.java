package local.ua;


import org.zoolu.sip.address.NameAddress;


/** Listener di UserAgent */
public interface UserAgentListener
{
	/** Quando una nuova chiama è in arrivo. */
   public void onUaCallIncoming(UserAgent ua, NameAddress callee, NameAddress caller);
   
   /** Quando una chiamata in arrivo viene cancellata. */
   public void onUaCallCancelled(UserAgent ua);

   /** Quando una chiamata in uscita sta squillando in remoto. */
   public void onUaCallRinging(UserAgent ua);
   
   /** Quando una chiamata in uscita è stata accettata */
   public void onUaCallAccepted(UserAgent ua);

   /** Quando una chiamata in uscita è stata rifiutata o è scaduto il timeuot*/
   public void onUaCallFailed(UserAgent ua);

   /** When a call has been locally or remotly closed */
   /** Quando una chiamata è stata chiusa in locale o in remoto */
   public void onUaCallClosed(UserAgent ua);
   
}