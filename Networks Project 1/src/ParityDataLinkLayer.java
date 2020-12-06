import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class ParityDataLinkLayer extends DataLinkLayer{
	@Override
    /**
     * Embed a raw sequence of bytes into a framed sequence.
     *
     * @param  data The raw sequence of bytes to be framed.
     * @return A complete frame.
     */
    protected byte[] createFrame (byte[] data) {
	// This simply declares an empty queue of bytes	
	Queue<Byte> framingData = new LinkedList<Byte>();
	
	int currentFrameSum = 0;
	// Begin with the start tag.
	framingData.add(startTag);

	// Add each byte of original data.
	for (int i = 0; i < data.length; i += 1) {  
		byte currentByte = data[i];
		//holds the value of every 6 data byte frame
		currentFrameSum = currentFrameSum + calParity(currentByte);
		
		// If the current data byte is itself a metadata tag, then precede
	    // it with an escape tag.
		if ((currentByte == startTag) || (currentByte == stopTag) || (currentByte == evenStopTag)|| 
			(currentByte == oddStopTag)|| (currentByte == escapeTag)) {
				framingData.add(escapeTag);
		}
		// Add the data byte itself.
	    framingData.add(currentByte);
	    // for every 6 bytes in the frame or the remaining bytes at the end
	    // if the sum of the past 6 bytes is even, then add a evenStopTag
	    // if odd then add the oddStopTag
	    if (i == data.length-1) {
	    	if(currentFrameSum%2 == 0) {
	    		framingData.add(evenStopTag);
	    		framingData.add(stopTag);
	    	}else {
	    		framingData.add(oddStopTag);
	    		framingData.add(stopTag);
	    	}
	    }
	}
	// Convert to the desired byte array.
	byte[] framedData = new byte[framingData.size()];
	Iterator<Byte>  i = framingData.iterator();
	int             j = 0;
	while (i.hasNext()) {
	    framedData[j++] = i.next();
	}
	return framedData;
    } 
	
	public int calParity(byte currentByte) {
		int bitNum = 0;
		for (int i = 0; i<8; i++) {
			byte myMask = (byte) (1<<i);
			if((currentByte & myMask) != (byte)(0)) {
				bitNum++;
			}
		}
		return bitNum;
	}	
	
    // =========================================================================


    
    // =========================================================================
    /**
     * Determine whether the received, buffered data constitutes a complete
     * frame.  If so, then remove the framing metadata and return the original
     * data.  Note that any data preceding an escaped start tag is assumed to be
     * part of a damaged frame, and is thus discarded.
     *
     * @return If the buffer contains a complete frame, the extracted, original
     * data; <code>null</code> otherwise.
     */
    protected byte[] processFrame () {

	// Search for a start tag.  Discard anything prior to it.

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
	/*boolean       stopTagFound = false; */
	int currentFrameSum = 0;
	while (/* !stopTagFound && */ i.hasNext()) {
		byte current = i.next();
		if (current == escapeTag) {
			if (i.hasNext()) {
			    current = i.next();
			    currentFrameSum = currentFrameSum + calParity(current);
			    extractedBytes.add(current);
			}
		}else if(current == evenStopTag){
			if (!(currentFrameSum%2 == 0)) {
				byte[] extractedData = new byte[extractedBytes.size()];
				int j = 0;
				i = extractedBytes.iterator();
				while (i.hasNext()) {
					extractedData[j] = i.next();
					j += 1;
				}
			System.out.println("ERROR DETECTED AT FRAME: " + extractedData.toString());
			cleanBufferUpTo(i);
			return null;
			
			}
			currentFrameSum = 0;
			
		}else if(current == oddStopTag) {
			if((currentFrameSum%2 == 0)) {
				byte[] extractedData = new byte[extractedBytes.size()];
				int j = 0;
				i = extractedBytes.iterator();
				while (i.hasNext()) {
					extractedData[j] = i.next();
					j += 1;
				}
			System.out.println("ERROR DETECTED AT FRAME: " + extractedData.toString());
			cleanBufferUpTo(i);
				return null;
			} 
			currentFrameSum = 0;
			// if we are at the final stop tag there should be nothing after it.
			// we have to check hasNext to make sure
		}else if(current == stopTag) {
			cleanBufferUpTo(i);
			byte[] extractedData = new byte[extractedBytes.size()];
			int j = 0;
			i = extractedBytes.iterator();
			while (i.hasNext()) {
				extractedData[j] = i.next();
				j += 1;
			}

			return extractedData;
				
		}else{	
			currentFrameSum = currentFrameSum + calParity(current);
			extractedBytes.add(current);
		}
	}
		return null;

    } // processFrame ()
    // ===============================================================



    // ===============================================================
    @ Override 
    public void send (byte[] data) {
    	// Call on the underlying physical layer to send the data.
    int index = 0;
    byte[] subData;
    	while(index < data.length) {
    		if(index + 6 < data.length-1) {
    			subData = Arrays.copyOfRange(data, index, index+6);
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
    private final byte oddStopTag = (byte)'-';
    private final byte evenStopTag = (byte)'+';
    private final byte escapeTag = (byte)'\\';
    // ===============================================================


}
