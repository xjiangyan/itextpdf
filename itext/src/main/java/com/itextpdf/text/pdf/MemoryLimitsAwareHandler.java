package com.itextpdf.text.pdf;

/**
 * A {@link MemoryLimitsAwareHandler} handles memory allocation and prevents decompressed pdf streams from occupation of more space than allowed.
 */
public class MemoryLimitsAwareHandler {

    private static final int SINGLE_SCALE_COEFFICIENT = 100;
    private static final int SUM_SCALE_COEFFICIENT = 500;

    private static final int SINGLE_DECOMPRESSED_PDF_STREAM_MIN_SIZE = Integer.MAX_VALUE / 100;
    private static final long SUM_OF_DECOMPRESSED_PDF_STREAMW_MIN_SIZE = Integer.MAX_VALUE / 20;

    private int maxSizeOfSingleDecompressedPdfStream;
    private long maxSizeOfDecompressedPdfStreamsSum;

    private long allMemoryUsedForDecompression = 0;
    private long memoryUsedForCurrentPdfStreamDecompression = 0;

    boolean considerCurrentPdfStream = false;

    /**
     * Creates a {@link MemoryLimitsAwareHandler} which will be used to handle decompression of pdf streams.
     * The max allowed memory limits will be generated by default.
     */
    public MemoryLimitsAwareHandler() {
        maxSizeOfSingleDecompressedPdfStream = SINGLE_DECOMPRESSED_PDF_STREAM_MIN_SIZE;
        maxSizeOfDecompressedPdfStreamsSum = SUM_OF_DECOMPRESSED_PDF_STREAMW_MIN_SIZE;
    }

    /**
     * Creates a {@link MemoryLimitsAwareHandler} which will be used to handle decompression of pdf streams.
     * The max allowed memory limits will be generated by default, based on the size of the document.
     *
     * @param documentSize the size of the document, which is going to be handled by iText.
     */
    public MemoryLimitsAwareHandler(long documentSize) {
        maxSizeOfSingleDecompressedPdfStream = (int) calculateDefaultParameter(documentSize, SINGLE_SCALE_COEFFICIENT, SINGLE_DECOMPRESSED_PDF_STREAM_MIN_SIZE);
        maxSizeOfDecompressedPdfStreamsSum = calculateDefaultParameter(documentSize, SUM_SCALE_COEFFICIENT, SUM_OF_DECOMPRESSED_PDF_STREAMW_MIN_SIZE);
    }

    /**
     * Gets the maximum allowed size which can be occupied by a single decompressed pdf stream.
     *
     * @return the maximum allowed size which can be occupied by a single decompressed pdf stream.
     */
    public int getMaxSizeOfSingleDecompressedPdfStream() {
        return maxSizeOfSingleDecompressedPdfStream;
    }

    /**
     * Sets the maximum allowed size which can be occupied by a single decompressed pdf stream.
     * This value correlates with maximum heap size. This value should not exceed limit of the heap size.
     *
     * iText will throw an exception if during decompression a pdf stream with two or more filters of identical type
     * occupies more memory than allowed.
     *
     * @param maxSizeOfSingleDecompressedPdfStream the maximum allowed size which can be occupied by a single decompressed pdf stream.
     * @return this {@link MemoryLimitsAwareHandler} instance.
     */
    public MemoryLimitsAwareHandler setMaxSizeOfSingleDecompressedPdfStream(int maxSizeOfSingleDecompressedPdfStream) {
        this.maxSizeOfSingleDecompressedPdfStream = maxSizeOfSingleDecompressedPdfStream;
        return this;
    }

    /**
     * Gets the maximum allowed size which can be occupied by all decompressed pdf streams.
     *
     * @return the maximum allowed size value which streams may occupy
     */
    public long getMaxSizeOfDecompressedPdfStreamsSum() {
        return maxSizeOfDecompressedPdfStreamsSum;
    }

    /**
     * Sets the maximum allowed size which can be occupied by all decompressed pdf streams.
     * This value can be limited by the maximum expected PDF file size when it's completely decompressed.
     * Setting this value correlates with the maximum processing time spent on document reading
     *
     * iText will throw an exception if during decompression pdf streams with two or more filters of identical type
     * occupy more memory than allowed.
     *
     * @param maxSizeOfDecompressedPdfStreamsSum he maximum allowed size which can be occupied by all decompressed pdf streams.
     * @return this {@link MemoryLimitsAwareHandler} instance.
     */
    public MemoryLimitsAwareHandler setMaxSizeOfDecompressedPdfStreamsSum(long maxSizeOfDecompressedPdfStreamsSum) {
        this.maxSizeOfDecompressedPdfStreamsSum = maxSizeOfDecompressedPdfStreamsSum;
        return this;
    }

    /**
     * Considers the number of bytes which are occupied by the decompressed pdf stream.
     * If memory limits have not been faced, throws an exception.
     *
     * @param numOfOccupiedBytes the number of bytes which are occupied by the decompressed pdf stream.
     * @return this {@link MemoryLimitsAwareHandler} instance.
     * @see {@link MemoryLimitsAwareException}
     */
    MemoryLimitsAwareHandler considerBytesOccupiedByDecompressedPdfStream(long numOfOccupiedBytes) {
        if (considerCurrentPdfStream) {
            if (memoryUsedForCurrentPdfStreamDecompression < numOfOccupiedBytes) {
                memoryUsedForCurrentPdfStreamDecompression = numOfOccupiedBytes;
                if (memoryUsedForCurrentPdfStreamDecompression > maxSizeOfSingleDecompressedPdfStream) {
                    throw new MemoryLimitsAwareException(MemoryLimitsAwareException.DuringDecompressionSingleStreamOccupiedMoreMemoryThanAllowed);
                }
            }
        }
        return this;
    }

    /**
     * Begins handling of current pdf stream decompression.
     *
     * @return this {@link MemoryLimitsAwareHandler} instance.
     */
    MemoryLimitsAwareHandler beginDecompressedPdfStreamProcessing() {
        ensureCurrentStreamIsReset();
        considerCurrentPdfStream = true;
        return this;
    }

    /**
     * Ends handling of current pdf stream decompression.
     * If memory limits have not been faced, throws an exception.
     *
     * @return this {@link MemoryLimitsAwareHandler} instance.
     * @see {@link MemoryLimitsAwareException}
     */
    MemoryLimitsAwareHandler endDecompressedPdfStreamProcessing() {
        allMemoryUsedForDecompression += memoryUsedForCurrentPdfStreamDecompression;
        if (allMemoryUsedForDecompression > maxSizeOfDecompressedPdfStreamsSum) {
            throw new MemoryLimitsAwareException(MemoryLimitsAwareException.DuringDecompressionMultipleStreamsInSumOccupiedMoreMemoryThanAllowed);
        }
        ensureCurrentStreamIsReset();
        considerCurrentPdfStream = false;
        return this;
    }

    long getAllMemoryUsedForDecompression() {
        return allMemoryUsedForDecompression;
    }

    private static long calculateDefaultParameter(long documentSize, int scale, long min) {
        long result = documentSize * scale;
        if (result < min) {
            result = min;
        }
        if (result > min * scale) {
            result = min * scale;
        }
        return result;
    }

    private void ensureCurrentStreamIsReset() {
        memoryUsedForCurrentPdfStreamDecompression = 0;
    }
}
