package local.media;


/** Listener di AudioClipPlayer.
* Cattura l'evento onAudioClipStop (), quando si arresta l'audio.  
*/
public interface AudioClipPlayerListener
{
	/** Quando viene stoppato l'audio. */
   public void onAudioClipStop(AudioClipPlayer player);
   
}


