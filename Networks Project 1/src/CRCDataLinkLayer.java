import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class CRCDataLinkLayer extends DataLinkLayer{
// Use the CRC checksum method to detect errors on each frame.
// Consult the text to select a good generator polynomial to drive your CRC. 
	public static byte CRCholder = 0b0;
	
	@Override
	   protected byte[] createFrame (byte[] data) {
		//This simply declares an empty queue of bytes	
		Queue<Byte> framingData = new LinkedList<Byte>();
		// Begin with the start tag.
		framingData.add(startTag);
		// Add each byte of original data.
		for (int i = 0; i < data.length; i += 1) {
			byte currentByte = data[i];
			//holds the value of every 6 data byte frame
			
			// If the current data byte is itself a metadata tag, then precede
		    // it with an escape tag.
			if ((currentByte == startTag) || (currentByte == stopTag)|| (currentByte == escapeTag)) {
					framingData.add(escapeTag);
			}
			framingData.add(currentByte);
		}
		// adds my CRC values 
		framingData.add((byte) calRem(boolInHelp(data)));
		framingData.add(stopTag);
		// Convert to the desired byte array.
		byte[] framedData = new byte[framingData.size()];
		Iterator<Byte>  i = framingData.iterator();
		int             j = 0;
		while (i.hasNext()) {
		    framedData[j++] = i.next();
		}
		return framedData;
	}	
	public static int calRem(LinkedList<Boolean> myC) {
		
		int gen = 0b1001001;
		int rem = 0;
		while(!myC.isEmpty()) {
			if (myC.poll()) {
				rem = (rem<<1)|1;
			}else {
				rem = (rem<<1);
			}
			if(Integer.highestOneBit(gen) == Integer.highestOneBit(rem)) {
				rem = rem^gen;
			}
			
		}
		return rem;
	}
	public static LinkedList<Boolean> boolInHelp(byte[]nuts) {
		LinkedList<Boolean> toReturn = new LinkedList<Boolean>(); 
		int j = 0;
		byte currentbyte;
		while(j < nuts.length) {
			currentbyte = nuts[j];
			for (int i = 0; i<8; i++) {
				byte myMask = (byte) (1<<i);
				if((currentbyte & myMask) != (0b0)) {
					toReturn.add(true);
				}else {
					toReturn.add(false);
				}
			}
		j++;
		// adds 6 0s at the end because of length of generator-1
		}
		for(int i = 0; i<6; i++) {
			toReturn.add(false);
		} 
		return toReturn;
	}
	@Override
	protected byte[] processFrame() {
		
		Iterator<Byte> i = byteBuffer.iterator();
		boolean startTagFound = false;
			
		while (!startTagFound && i.hasNext()) {
		    byte current = i.next();
		    if (current != startTag){
			i.remove();
		    } else {
			startTagFound = true;
		    }
		}	
		if (!startTagFound) {
		    return null;
		} 
		// Try to extract data while waiting for an unescaped stop tag.
		Queue<Byte> extractedBytes = new LinkedList<Byte>();
		while ( i.hasNext()) {
			byte current = i.next();
			if (current == escapeTag) {
				if (i.hasNext()) {
				    current = i.next(); 
				    extractedBytes.add(current);
				    CRCholder = current;
				}
				CRCholder = current;
			}else if(current == stopTag) {
				cleanBufferUpTo(i); 
				byte[] extractedData = new byte[extractedBytes.size()-1];
				int j = 0;
				i = extractedBytes.iterator();
				while (j <= extractedData.length-1 /* i.hasNext()*/ ) {
					extractedData[j] = i.next();
					j += 1;
				}
				if( CRCholder != calRem(boolInHelp(extractedData))) {
					System.out.println("ERROR AT FRAME: "+ extractedData.toString());
					return null;
				}else {
					return extractedData;
				}		
			}else{	
				CRCholder = current;
				extractedBytes.add(current);
			}
		}
			return null;
	}
	
	 @ Override 
	    public void send (byte[] data) {
	    	// Call on the underlying physical layer to send the data.
	    int index = 0;
	    byte[] subData;
	    	while(index < data.length) {
	    		if(index + 6 < data.length-1) {
	    			subData = Arrays.copyOfRange(data, index, index+6);
	    			//if(index + 6 < data.length)
	    		} else { 
	    			subData = Arrays.copyOfRange(data, index, data.length);
	    			index = data.length;
	    		}
	    		index = index+6;
	    		byte[] framedData = createFrame(subData);
	    		for (int i = 0; i < framedData.length; i += 1) {
	    			transmit(framedData[i]);
	    		}
	    	}
	    } 
	 
private void cleanBufferUpTo (Iterator<Byte> end) {
	Iterator<Byte> i = byteBuffer.iterator();
	while (i.hasNext() && i != end) {
	    i.next();
	    i.remove();
	}

    }


// ===============================================================



// ===============================================================
// DATA MEMBERS
// ===============================================================



// ===============================================================
// The start tag, stop tag, and the escape tag.
private final byte startTag  = (byte)'{';
private final byte stopTag   = (byte)'}';
private final byte escapeTag = (byte)'\\';
// ===============================================================
}