/*
 * Copyright (c) [2016] [ <ether.camp> ]
 * This file is part of the ethereumJ library.
 *
 * The ethereumJ library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ethereumJ library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ethereumJ library. If not, see <http://www.gnu.org/licenses/>.
 */

package org.tron.core.services.interfaceOnSolidity;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.core.config.args.Args;
import org.tron.core.db.Manager;

@Slf4j(topic = "API")
@Component
public class WalletOnSolidity {

  private ListeningExecutorService executorService = MoreExecutors.listeningDecorator(
          Executors.newFixedThreadPool(Args.getInstance().getSolidityThreads(),
                  new ThreadFactoryBuilder().setNameFormat("WalletOnSolidity-%d").build()));

  @Autowired
  private Manager dbManager;

  public <T> T futureGet(TronCallable<T> callable) {
    try {
      dbManager.setMode(false);
      return callable.call();
    } finally {
      dbManager.setMode(true);
    }
  }

  public <T> T futureGetWithoutTimeout(Callable<T> callable) {
    ListenableFuture<T> future = executorService.submit(() -> {
      try {
        dbManager.setMode(false);
        return callable.call();
      } catch (Exception e) {
        logger.info("futureGet " + e.getMessage());
        return null;
      }
    });

    try {
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException ignored) {
    }

    return null;
  }

  public void futureGet(Runnable runnable) {
    try {
      dbManager.setMode(false);
      runnable.run();
    } finally {
      dbManager.setMode(true);
    }
  }


  public void futureGetWithoutTimeout(Runnable runnable) {
    ListenableFuture<?> future = executorService.submit(() -> {
      try {
        dbManager.setMode(false);
        runnable.run();
      } catch (Exception e) {
        logger.info("futureGet " + e.getMessage());
      }
    });

    try {
      future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException ignored) {
    }
  }

  public interface TronCallable<T> extends Callable<T> {
    @Override
    T call();
  }
}
