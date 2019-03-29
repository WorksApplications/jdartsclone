/*
 * Copyright (c) 2017 Works Applications Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.worksap.nlp.dartsclone;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

import com.worksap.nlp.dartsclone.details.DoubleArrayBuilder;
import com.worksap.nlp.dartsclone.details.KeySet;

/**
 * A trie structure using Double-Array.
 *
 * This class provides storage of key-value pairs.
 * The key is an array of bytes, unique, and does not include {@code \0}.
 * The value is a non-negative integer.
 */
public class DoubleArray {

    private IntBuffer array;
    private ByteBuffer buffer;
    private int size;           // number of elements

    /**
     * Set an integer array as structures of Darts-clone.
     *
     * @param array the structures of Darts-clone
     * @param size the number of elements
     */
    public void setArray(IntBuffer array, int size) {
        this.array = array;
        this.size = size;
    }

    /**
     * Returns the structures of Darts-clone as an array of integer.
     *
     * @return the structures of Darts-clone
     */
    public IntBuffer array() {
        return array;
    }

    /**
     * Returns the structures of Darts-clone as an array of byte
     *
     * @return the structures of Darts-clone
     */
    public ByteBuffer byteArray() {
        return buffer;
    }

    /**
     * Removes all structures.
     */
    public void clear() {
        buffer = null;
        size = 0;
    }

    /**
     * Returns the number of the internal elements in the structures of
     * Darts-clone.
     *
     * It is not the number of keys in the trie.
     *
     * @return the number of elements
     */
    public int size() { return size; }

    /**
     * Returns the size of the storage used in the structures of Darts-clone.
     *
     * @return the number of elements
     */
    public int totalSize() { return 4 * size; }

    /**
     * Builds the trie from the pairs of key and value.
     *
     * When a pair of key and value is added to trie, {@code progressFunction}
     * is called with the number of added pairs and the total of pairs.
     *
     * @param keys key with which the specified value is to be associated
     * @param values value to be associated with the specified key
     * @param progressFunction the call for showing the progress of building
     */
    public void build(byte[][] keys, int[] values,
                      BiConsumer<Integer, Integer> progressFunction) {
        KeySet keySet = new KeySet(keys, values);
        DoubleArrayBuilder builder = new DoubleArrayBuilder(progressFunction);
        builder.build(keySet);
        buffer = builder.copy();
        array = buffer.asIntBuffer();
        size = array.capacity();
    }

    /**
     * Reads the trie from the file.
     *
     * @param inputFile the file to read
     * @param position the offset to read
     * @param totalSize the size to read
     * @throws IOException if reading a file is failed
     */
    public void open(FileChannel inputFile, long position, long totalSize)
        throws IOException {

        if (position < 0) {
            position = 0;
        }
        if (totalSize <= 0) {
            totalSize = inputFile.size();
        }
        buffer = inputFile.map(FileChannel.MapMode.READ_ONLY,
                               position, totalSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        array = buffer.asIntBuffer();
        size = array.capacity();
    }

    /**
     * Writes this trie to the file.
     *
     * @param outputFile the file to write
     * @throws IOException if writing a file is failed
     */
    public void save(FileChannel outputFile) throws IOException {
        outputFile.write(buffer);
    }

    /**
     * Returns the value to which the specified key is mapped, or
     * a negative integer if this trie contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     * @return the array of integer includes the value to which the specified
     *         key is mapped and the length of the key
     */
    public int[] exactMatchSearch(byte[] key) {
        int[] result = new int[] { -1, 0 };
        int nodePos = 0;
        int unit = array.get(nodePos);

        for (byte k : key) {
            nodePos ^= offset(unit) ^ Byte.toUnsignedInt(k);
            unit = array.get(nodePos);
            if (label(unit) != Byte.toUnsignedInt(k)) {
                return result;
            }
        }
        if (!hasLeaf(unit)) {
            return result;
        }
        unit = array.get(nodePos ^ offset(unit));
        result[0] = value(unit);
        result[1] = key.length;
        return result;
    }

    /**
     * Returns the values to which the prefixes of the specified key is mapped.
     *
     * If {@code offset} is not 0, the key is evaluated as the sub array
     * removed the first {@code offset} bytes.
     *
     * @param key the key whose associated value is to be returned
     * @param offset the offset of the key
     * @param maxNumResult the maximum size of the list
     * @return the list of the array of integer includes the value to which
     *         the prefix of the specified key is mapped and the length
     *         of the prefix
     */
    public List<int[]> commonPrefixSearch(byte[] key, int offset,
                                          int maxNumResult) {
        List<int[]> result = new ArrayList<>();

        int nodePos = 0;
        int unit = array.get(nodePos);
        nodePos ^= offset(unit);
        for (int i = offset; i < key.length; i++) {
            byte k = key[i];
            nodePos ^= Byte.toUnsignedInt(k);
            unit = array.get(nodePos);
            if (label(unit) != Byte.toUnsignedInt(k)) {
                return result;
            }
            
            nodePos ^= offset(unit);
            if (hasLeaf(unit) && result.size() < maxNumResult) {
                int[] r = new int[] { value(array.get(nodePos)), i + 1 };
                result.add(r);
            }
        }
        return result;
    }

    /**
     * Returns the values to which the prefixes of the specified key is mapped.
     *
     * This method is equivalent to {@code commonPrefixSearch().iterator()}.
     *
     * If {@code offset} is not 0, the key is evaluated as the sub array
     * removed the first {@code offset} bytes.
     *
     * @param key the key whose associated value is to be returned
     * @param offset the offset of the key
     * @return the list of the array of integer includes the value to which
     *         the prefix of the specified key is mapped and the length
     *         of the prefix
     */
    public Iterator<int[]> commonPrefixSearch(byte[] key, int offset) {
        return new Itr(key, offset);
    }

    private class Itr implements Iterator<int[]> {
        private final byte[] key;
        private int offset;
        private int nodePos;
        private int[] next;

        Itr(byte[] key, int offset) {
            this.key = key;
            this.offset = offset;
            nodePos = 0;
            int unit = array.get(nodePos);
            nodePos ^= offset(unit);
            next = null;
        }

        @Override
        public boolean hasNext() {
            if (next == null) {
                next = getNext();
            }
            return (next != null);
        }

        @Override
        public int[] next() {
            int[] r = (next != null) ? next : getNext();
            next = null;
            if (r == null) {
                throw new NoSuchElementException();
            }
            return r;
        }

        int[] getNext() {
            for ( ; offset < key.length; offset++) {
                byte k = key[offset];
                nodePos ^= Byte.toUnsignedInt(k);
                int unit = array.get(nodePos);
                if (label(unit) != Byte.toUnsignedInt(k)) {
                    offset = key.length; // no more loop
                    return null;
                }

                nodePos ^= offset(unit);
                if (hasLeaf(unit)) {
                    int[] r = new int[] { value(array.get(nodePos)), ++offset };
                    return r;
                }
            }
            return null;
        }
    }

    private boolean hasLeaf(int unit) {
        return ((unit >>> 8) & 1) == 1;
    }
        
    private int value(int unit) {
        return unit & ((1 << 31) - 1);
    }

    private int label(int unit) {
        return unit & ((1 << 31) | 0xFF);
    }

    private int offset(int unit) {
        return ((unit >>> 10) << ((unit & (1 << 9)) >>> 6));
    }
}
