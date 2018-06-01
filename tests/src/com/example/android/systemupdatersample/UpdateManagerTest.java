/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.systemupdatersample;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.example.android.systemupdatersample.tests.R;
import com.example.android.systemupdatersample.util.PayloadSpecs;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Tests for {@link UpdateManager}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class UpdateManagerTest {

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private UpdateEngine mUpdateEngine;
    @Mock
    private PayloadSpecs mPayloadSpecs;
    private UpdateManager mSubject;
    private Context mContext;
    private UpdateConfig mNonStreamingUpdate003;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getContext();
        mSubject = new UpdateManager(mUpdateEngine, mPayloadSpecs);
        mNonStreamingUpdate003 =
                UpdateConfig.fromJson(readResource(R.raw.update_config_003_nonstream));
    }

    @Test
    public void applyUpdate_appliesPayloadToUpdateEngine() throws Exception {
        PayloadSpec payload = buildMockPayloadSpec();
        when(mPayloadSpecs.forNonStreaming(any(File.class))).thenReturn(payload);
        when(mUpdateEngine.bind(any(UpdateEngineCallback.class))).thenAnswer(answer -> {
            // When UpdateManager is bound to update_engine, it passes
            // UpdateEngineCallback as a callback to update_engine.
            UpdateEngineCallback callback = answer.getArgument(0);
            callback.onStatusUpdate(
                    UpdateEngine.UpdateStatusConstants.IDLE,
                    /*engineProgress*/ 0.0f);
            return null;
        });

        mSubject.bind();
        mSubject.applyUpdate(null, mNonStreamingUpdate003);

        verify(mUpdateEngine).applyPayload(
                "file://blah",
                120,
                340,
                new String[] {
                        "SWITCH_SLOT_ON_REBOOT=0" // ab_config.force_switch_slot = false
                });
    }

    @Test
    public void stateIsRunningAndEngineStatusIsIdle_reApplyLastUpdate() throws Exception {
        PayloadSpec payload = buildMockPayloadSpec();
        when(mPayloadSpecs.forNonStreaming(any(File.class))).thenReturn(payload);
        when(mUpdateEngine.bind(any(UpdateEngineCallback.class))).thenAnswer(answer -> {
            // When UpdateManager is bound to update_engine, it passes
            // UpdateEngineCallback as a callback to update_engine.
            UpdateEngineCallback callback = answer.getArgument(0);
            callback.onStatusUpdate(
                    UpdateEngine.UpdateStatusConstants.IDLE,
                    /*engineProgress*/ 0.0f);
            return null;
        });

        mSubject.bind();
        mSubject.applyUpdate(null, mNonStreamingUpdate003);
        mSubject.unbind();
        mSubject.bind(); // re-bind - now it should re-apply last update

        assertEquals(mSubject.getUpdaterState(), UpdaterState.RUNNING);
        // it should be called 2 times
        verify(mUpdateEngine, times(2)).applyPayload(
                "file://blah",
                120,
                340,
                new String[] {
                        "SWITCH_SLOT_ON_REBOOT=0" // ab_config.force_switch_slot = false
                });
    }

    private PayloadSpec buildMockPayloadSpec() {
        PayloadSpec payload = mock(PayloadSpec.class);
        when(payload.getUrl()).thenReturn("file://blah");
        when(payload.getOffset()).thenReturn(120L);
        when(payload.getSize()).thenReturn(340L);
        when(payload.getProperties()).thenReturn(ImmutableList.of());
        return payload;
    }

    private String readResource(int id) throws IOException {
        return CharStreams.toString(new InputStreamReader(
                mContext.getResources().openRawResource(id)));
    }

}
