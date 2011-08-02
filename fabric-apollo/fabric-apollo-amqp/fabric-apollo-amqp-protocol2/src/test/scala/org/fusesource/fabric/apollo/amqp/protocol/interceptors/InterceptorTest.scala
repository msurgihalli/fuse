/*
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved
 *
 *    http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license, a copy of which has been included with this distribution
 * in the license.txt file
 */

package org.fusesource.fabric.apollo.amqp.protocol.interceptors

import org.apache.activemq.apollo.util.FunSuiteSupport
import org.scalatest.matchers.ShouldMatchers
import org.fusesource.fabric.apollo.amqp.codec.interfaces.AMQPFrame
import collection.mutable.Queue
import org.fusesource.fabric.apollo.amqp.protocol.commands.SimpleFrame
import test_interceptors._
import org.fusesource.fabric.apollo.amqp.protocol.interfaces.Interceptor._
import org.fusesource.hawtdispatch._
import org.fusesource.fabric.apollo.amqp.protocol.utilities.Tasks

/**
 *
 */
class InterceptorTest extends FunSuiteSupport with ShouldMatchers {

  test("Test get head/tail of interceptor chain") {
    val middle = new SimpleInterceptor
    middle.incoming = new SimpleInterceptor
    middle.outgoing = new SimpleInterceptor

    val start = middle.outgoing
    val end = middle.incoming

    start.tail should be theSameInstanceAs (end)
    end.head should be theSameInstanceAs (start)
    middle.tail should be theSameInstanceAs (end)
    middle.head should be theSameInstanceAs (start)

    middle.remove

    middle.tail should be theSameInstanceAs  (middle)
    middle.head should be theSameInstanceAs (middle)

    start.tail should be theSameInstanceAs (end)
    end.head should be theSameInstanceAs (start)

  }

  test("Create interceptor chain, remove an interceptor, verify it's disconnected") {
    val middle = new SimpleInterceptor
    middle.incoming = new SimpleInterceptor
    middle.outgoing = new SimpleInterceptor

    val start = middle.outgoing
    val end = middle.incoming

    middle.remove

    start.incoming should be theSameInstanceAs (end)
    end.outgoing should be theSameInstanceAs (start)
    middle.connected should be (false)
  }

  test("Create interceptor chain, send message down it, modify chain, send another message down it") {
    var got_here = false
    val in = new TestSendInterceptor((frame:AMQPFrame, tasks:Queue[() => Unit]) => {
      frame should not be (null)
      frame.isInstanceOf[SimpleFrame] should be (true)
      got_here = true
    })

    in.outgoing = new TaskExecutingInterceptor
    in.incoming = new SimpleInterceptor
    in.incoming.incoming = new SimpleInterceptor
    in.incoming.incoming.incoming = new SimpleInterceptor
    in.incoming.incoming.incoming.incoming = new TerminationInterceptor

    in.receive(new SimpleFrame, Tasks())
    got_here should be (true)

    got_here = false
    in.incoming.incoming.incoming.remove
    in.incoming.incoming.remove

    in.receive(new SimpleFrame, Tasks())
    got_here should be (true)

  }

  test("toString") {
    val in = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor
    in.tail.incoming = new TerminationInterceptor
    in.tail.incoming = new TaskExecutingInterceptor
    in.tail.incoming = new TerminationInterceptor
    info("Chain is %s", display_chain(in))
  }

  test("set dispatch queue on chain") {
    val in = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor
    in.tail.incoming = new SimpleInterceptor

    in.foreach((x) => {x.queue_set should be (false); Unit})

    in.queue = Dispatch.createQueue

    in.foreach((x) => {x.queue_set should be (true); Unit})

    in.tail.incoming = new SimpleInterceptor

    in.foreach((x) => {x.queue_set should be (true); Unit})

    val middle = in.incoming.incoming
    middle.remove
    in.foreach((x) => {x.queue_set should be (true); Unit})
    middle.queue_set should be (false)

    val in2 = new SimpleInterceptor
    in2.tail.incoming = new SimpleInterceptor
    in2.tail.incoming = new SimpleInterceptor

    in2.foreach((x) => {x.queue_set should be (false); Unit})

    in.tail.incoming = in2

    in.foreach((x) => {x.queue_set should be (true); Unit})
    in2.foreach((x) => {x.queue_set should be (true); Unit})
  }

}