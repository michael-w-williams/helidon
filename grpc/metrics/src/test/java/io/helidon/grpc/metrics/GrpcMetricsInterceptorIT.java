/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.grpc.metrics;

import java.util.Map;

import io.helidon.common.CollectionsHelper;
import io.helidon.grpc.server.GrpcService;
import io.helidon.grpc.server.MethodDescriptor;
import io.helidon.grpc.server.ServiceDescriptor;
import io.helidon.metrics.MetricsSupport;
import io.helidon.metrics.RegistryFactory;
import io.helidon.webserver.Routing;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Meter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Timer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A test for the {@link io.helidon.grpc.metrics.GrpcMetrics} interceptor.
 * <p>
 * This test runs as an integration test because it causes Helidon metrics
 * to be initialised which may impact other tests that rely on metrics being
 * configured a specific way.
 */
@SuppressWarnings("unchecked")
public class GrpcMetricsInterceptorIT {

    private static MetricRegistry vendorRegsistry;

    private static MetricRegistry appRegistry;

    private static Meter vendorMeter;

    private static Counter vendorCounter;

    private long vendorMeterCount;

    private long vendorCount;

    @BeforeAll
    static void configureMetrics() {
        Routing.Rules rules = Routing.builder().get("metrics");
        MetricsSupport.create().update(rules);

        vendorRegsistry = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.VENDOR);
        appRegistry = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION);
        vendorMeter = vendorRegsistry.meter("grpc.requests.meter");
        vendorCounter = vendorRegsistry.counter("grpc.requests.count");
    }

    @BeforeEach
    public void setup() {
        // obtain the current counts for vendor metrics so that we can assert
        // the count in each test
        vendorCount = vendorCounter.getCount();
        vendorMeterCount = vendorMeter.getCount();
    }

    @Test
    public void shouldUseCountedMetric() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("testCounted", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("testCounted");
        GrpcMetrics metrics = GrpcMetrics.counted();

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Counter appCounter = appRegistry.counter("Foo.testCounted");

        assertVendorMetrics();
        assertThat(appCounter.getCount(), is(1L));
    }

    @Test
    public void shouldUseHistogramMetric() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("barHistogram", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("barHistogram");
        GrpcMetrics metrics = GrpcMetrics.histogram();

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Histogram appHistogram = appRegistry.histogram("Foo.barHistogram");

        assertVendorMetrics();
        assertThat(appHistogram.getCount(), is(1L));
    }

    @Test
    public void shouldUseMeteredMetric() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("barMetered", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("barMetered");
        GrpcMetrics metrics = GrpcMetrics.metered();

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Meter appMeter = appRegistry.meter("Foo.barMetered");

        assertVendorMetrics();
        assertThat(appMeter.getCount(), is(1L));
    }

    @Test
    public void shouldUseTimerMetric() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("barTimed", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("barTimed");
        GrpcMetrics metrics = GrpcMetrics.timed();

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Timer appTimer = appRegistry.timer("Foo.barTimed");

        assertVendorMetrics();
        assertThat(appTimer.getCount(), is(1L));
    }

    @Test
    public void shouldApplyTags() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("barTags", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("barTags");
        Map<String, String> tags = CollectionsHelper.mapOf("one", "t1", "two", "t2");
        GrpcMetrics metrics = GrpcMetrics.counted().tags(tags);

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Counter appCounter = appRegistry.counter("Foo.barTags");

        assertVendorMetrics();
        assertThat(appCounter.toString(), containsString("tags='{one=t1, two=t2}'"));
    }

    @Test
    public void shouldApplyDescription() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("barDesc", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("barDesc");
        GrpcMetrics metrics = GrpcMetrics.counted().description("foo");

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Counter appCounter = appRegistry.counter("Foo.barDesc");

        assertVendorMetrics();
        assertThat(appCounter.toString(), containsString("description='foo'"));
    }

    @Test
    public void shouldApplyUnits() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("barUnits", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("barUnits");
        GrpcMetrics metrics = GrpcMetrics.counted().units(MetricUnits.BITS);

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Counter appCounter = appRegistry.counter("Foo.barUnits");

        assertVendorMetrics();
        assertThat(appCounter.toString(), containsString("unit='bits'"));
    }

    @Test
    public void shouldHaveCorrectNameWithDotsForSlashes() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .name("My/Service")
                .unary("bar", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("bar");
        GrpcMetrics metrics = GrpcMetrics.counted();

        ServerCall<String, String> call = call(metrics, descriptor, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Counter appCounter = appRegistry.counter("My.Service.bar");

        assertVendorMetrics();
        assertThat(appCounter.getCount(), is(1L));
    }

    @Test
    public void shouldUseNameFunction() throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService())
                .unary("barUnits", this::dummyUnary)
                .build();

        MethodDescriptor methodDescriptor = descriptor.method("barUnits");
        GrpcMetrics metrics = GrpcMetrics.counted().nameFunction((svc, method, type) -> "overridden");

        ServerCall<String, String> call = call(metrics, methodDescriptor);

        call.close(Status.OK, new Metadata());

        Counter appCounter = appRegistry.counter("overridden");

        assertVendorMetrics();
        assertThat(appCounter.getCount(), is(1L));
    }

    private ServerCall<String, String> call(GrpcMetrics metrics, MethodDescriptor methodDescriptor) throws Exception {
        ServiceDescriptor descriptor = ServiceDescriptor.builder(createMockService()).build();
        return call(metrics, descriptor, methodDescriptor);
    }

    private ServerCall<String, String> call(GrpcMetrics metrics,
                                            ServiceDescriptor descriptor,
                                            MethodDescriptor methodDescriptor) throws Exception {
        Metadata headers = new Metadata();
        ServerCall<String, String> call = mock(ServerCall.class);
        ServerCallHandler<String, String> next = mock(ServerCallHandler.class);
        ServerCall.Listener<String> listener = mock(ServerCall.Listener.class);

        when(call.getMethodDescriptor()).thenReturn(methodDescriptor.descriptor());
        when(next.startCall(any(ServerCall.class), any(Metadata.class))).thenReturn(listener);

        Context context = Context.ROOT.withValue(ServiceDescriptor.SERVICE_DESCRIPTOR_KEY, descriptor);

        ServerCall.Listener<String> result = context.call(() -> metrics.interceptCall(call, headers, next));

        assertThat(result, is(sameInstance(listener)));

        ArgumentCaptor<ServerCall> captor = ArgumentCaptor.forClass(ServerCall.class);

        verify(next).startCall(captor.capture(), same(headers));

        ServerCall<String, String> wrappedCall = captor.getValue();

        assertThat(wrappedCall, is(notNullValue()));

        return wrappedCall;
    }

    private void assertVendorMetrics() {
        Meter meter = vendorRegsistry.meter("grpc.requests.meter");
        Counter counter = vendorRegsistry.counter("grpc.requests.count");

        assertThat(meter.getCount(), is(vendorMeterCount + 1));
        assertThat(counter.getCount(), is(vendorCount + 1));
    }

    private GrpcService createMockService() {
        GrpcService service = mock(GrpcService.class);

        when(service.name()).thenReturn("Foo");

        return service;
    }

    private void dummyUnary(String request, StreamObserver<String> observer) {
    }
}
