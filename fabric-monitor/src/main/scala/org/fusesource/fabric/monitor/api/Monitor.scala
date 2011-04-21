/**
 * Copyright (C) 2010-2011, FuseSource Corp.  All rights reserved.
 *
 *     http://fusesource.com
 *
 * The software in this package is published under the terms of the
 * CDDL license a copy of which has been included with this distribution
 * in the license.txt file.
 */

package org.fusesource.fabric.monitor.api

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
trait Monitor {

  /**
   * Updates the monitor's configuration with the data sources that need
   * to be monitored.
   */
  def configure( value:Traversable[MonitoredSetDTO] ):Unit

  def close:Unit


  var poller_factories:Seq[PollerFactory]
}