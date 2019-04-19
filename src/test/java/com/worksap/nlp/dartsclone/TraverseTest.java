/*
 * Copyright (c) 2019 Works Applications Co., Ltd.
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

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TraverseTest {

    DoubleArray dic;

    static final byte[] A = "a".getBytes(StandardCharsets.UTF_8);
    static final byte[] AB = "ab".getBytes(StandardCharsets.UTF_8);
    static final byte[] AC = "ac".getBytes(StandardCharsets.UTF_8);
    static final byte[] B = "b".getBytes(StandardCharsets.UTF_8);
    static final byte[] C = "c".getBytes(StandardCharsets.UTF_8);
    static final byte[] CD = "cd".getBytes(StandardCharsets.UTF_8);

    @Before
    public void setUp() {
        byte[][] keys = new byte[][] { A, AB, CD };
        dic = new DoubleArray();
        dic.build(keys, null, null);
    }

    @Test
    public void traverse() {
        DoubleArray.TraverseResult r = dic.traverse(A, 0, 0);
        assertTrue(r.result >= 0);
        assertEquals(1, r.offset);
        DoubleArray.TraverseResult r2 = dic.traverse(B, 0, r.nodePosition);
        assertTrue(r2.result >= 0);
        assertEquals(1, r2.offset);
        DoubleArray.TraverseResult r3 = dic.traverse(AB, 1, r.nodePosition);
        assertTrue(r3.result >= 0);
        assertEquals(2, r3.offset);
    }

    @Test
    public void traverseNotMatchWithEnd() {
        DoubleArray.TraverseResult r = dic.traverse(C, 0, 0);
        assertEquals(-1, r.result);
        assertEquals(1, r.offset);
        DoubleArray.TraverseResult r2 = dic.traverse(CD, 1, r.nodePosition);
        assertTrue(r2.result >= 0);
        assertEquals(2, r2.offset);
    }

    @Test
    public void traverseNotMatchMiddle() {
        DoubleArray.TraverseResult r = dic.traverse(AC, 0, 0);
        assertEquals(-2, r.result);
        assertEquals(1, r.offset);
        DoubleArray.TraverseResult r2 = dic.traverse(B, 0, r.nodePosition);
        assertTrue(r2.result >= 0);
        assertEquals(1, r2.offset);
    }
}
