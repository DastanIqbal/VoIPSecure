package local.media;


import javax.sound.sampled.*;
import java.io.*;


/** AudioOutput consente l'accesso al sistema dell'audio in output, utilizzando javax.sound.
*/
public class AudioOutput
{


   static boolean DEBUG=false;
   
   static final int INTERNAL_BUFFER_SIZE=40960;
   
   private static SourceDataLine source_line;

   
   /** Inizializza il sistema audio in output. */
   public static void initAudioLine()
   {
      
      float fFrameRate=8000.0F;
      AudioFormat format=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fFrameRate, 16, 1, 2, fFrameRate, false);
      
      DataLine.Info lineInfo=new DataLine.Info(SourceDataLine.class,format,INTERNAL_BUFFER_SIZE);

      if (!AudioSystem.isLineSupported(lineInfo))
      {  System.err.println("ERROR: AudioLine not supported by this System.");
      }
      try
      {  source_line=(SourceDataLine)AudioSystem.getLine(lineInfo);
         if (DEBUG) println("SourceDataLine: "+source_line);
         source_line.open(format,INTERNAL_BUFFER_SIZE);
      }
      catch (LineUnavailableException e) 
      {  System.err.println("ERROR: LineUnavailableException at AudioReceiver()");
         e.printStackTrace();
      }      
   }          

   /** Chiude il sistema audio in output*/
   static public void closeAudioLine()
   {  source_line.close();
   }




   AudioOutputStream audio_output_stream=null;


   /** Costruttore di un AudioOutput con audio_format=[8000 Hz, ULAW, 8bit, Mono]*/
   public AudioOutput()
   {  init(null);
   }          

   /** Costruisce un AudioOutput. */
   public AudioOutput(AudioFormat audio_format)
   {  init(audio_format);
   }          

   /**Inizializza un AutioOutput. */
   private void init(AudioFormat format)
   {
      if (source_line==null) initAudioLine();
      
      if (format==null) 
      {  // by default use 8000 Hz, ULAW, 8bit, Mono
         float fFrameRate=8000.0F;
         format=new AudioFormat(AudioFormat.Encoding.ULAW, fFrameRate, 8, 1, 1, fFrameRate, false);
      }
      
      if (source_line.isOpen())
      {  // convert the audio stream to the selected format
         try
         {  audio_output_stream=new AudioOutputStream(source_line,format);
         }
         catch (Exception e)
         {  e.printStackTrace();
         }
      }
      else
      {  System.err.print("WARNING: Audio init error: source line is not open.");
      }
      
   } 
   
   
   /** Restituisce l'audio come OutputStream. */
   public OutputStream getOuputStream()
   {  //return output_stream;
      return audio_output_stream;
   }
  

   /** Starts playing */
   public void play()
   {  if (source_line.isOpen()) source_line.start();
      else
      {  System.err.print("WARNING: Audio play error: source line is not open.");
      }
   }


   /** Stops playing */
   public void stop()
   {  if (source_line.isOpen())
      {  source_line.drain();
         source_line.stop();
      }
      else
      {  System.err.print("WARNING: Audio stop error: source line is not open.");
      }
      //source_line.close();
   }


   /** Debug output */
   private static void println(String str)
   {  System.out.println("AudioOutput: "+str);
   }
   
}


