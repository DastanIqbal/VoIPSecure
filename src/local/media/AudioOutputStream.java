
package local.media;


import javax.sound.sampled.*;
import java.io.*;


/** AudioOutputStream è l'equivalente di javax.sound.sampled.AudioInputStream
 * per l' audio playout.
 */
class AudioOutputStream extends OutputStream
{
   static final int INTERNAL_BUFFER_SIZE=40960;

   /** The SourceDataLine */
   protected SourceDataLine source_line;

   /** Converted InputStream. */
   protected InputStream input_stream;

   /** Piped OutputStream. */
   protected OutputStream output_stream;

   /** Internal buffer. */
   private byte[] buff=new byte[INTERNAL_BUFFER_SIZE];


   /** Crea un nuovo AudioOutputStream da SourceDataLin. */
   public AudioOutputStream(SourceDataLine source_line)
   {  this.source_line=source_line;
      input_stream=null;
      output_stream=null;
   }
   
   /** Crea un nuovo AudioOutputStream fa un SourceDataLine convertendo il formato dell'audio. */
   public AudioOutputStream(SourceDataLine source_line, AudioFormat format) throws IOException
   {  this.source_line=source_line;

      PipedInputStream piped_input_stream=new PipedInputStream();
      output_stream=new PipedOutputStream(piped_input_stream);

      AudioInputStream audio_input_stream=new AudioInputStream(piped_input_stream,format,-1);
      if (audio_input_stream==null)
      {  String err="Failed while creating a new AudioInputStream.";
         throw new IOException(err);
      }
      
      input_stream=AudioSystem.getAudioInputStream(source_line.getFormat(),audio_input_stream);
      if (input_stream==null)
      {  String err="Failed while getting a transcoded AudioInputStream from AudioSystem.";
         err+="\n       input codec: "+format.toString();
         err+="\n       output codec:"+source_line.getFormat().toString();
         throw new IOException(err);
      }
   }
   
   /** Chiude l' output stream. */
   public void close()
   {  //source_line.close();
   }
   
   /** Svuota l'output stream e forza tutti i buffered output alla scrittura */
   public void flush()
   {  source_line.flush();
   }
   
   /** Scrive b.length bytes dal byte array all'output stream. */
   public void write(byte[] b) throws IOException
   {  if (output_stream!=null)
      {  output_stream.write(b);
         int len=input_stream.read(buff,0,buff.length);
         source_line.write(buff,0,len);
      }
      else
      {  source_line.write(b,0,b.length);
      }
   }
   /** Scrive il byte b nell' output stream. */ 
   public void write(int b) throws IOException
   {  if (output_stream!=null)
      {  output_stream.write(b);
         int len=input_stream.read(buff,0,buff.length);
         source_line.write(buff,0,len);
      }
      else
      {  buff[0]=(byte)b;
         source_line.write(buff,0,1);
      }
   }

}
