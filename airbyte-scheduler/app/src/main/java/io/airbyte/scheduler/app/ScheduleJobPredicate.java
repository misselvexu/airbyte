/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.scheduler.app;

import io.airbyte.config.StandardSync;
import io.airbyte.config.helpers.ScheduleHelpers;
import io.airbyte.scheduler.models.Job;
import io.airbyte.scheduler.models.JobStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class ScheduleJobPredicate implements BiPredicate<Optional<Job>, StandardSync> {

  private final Supplier<Instant> timeSupplier;

  public ScheduleJobPredicate(final Supplier<Instant> timeSupplier) {
    this.timeSupplier = timeSupplier;
  }

  @Override
  public boolean test(final Optional<Job> previousJobOptional, final StandardSync standardSync) {
    // if manual scheduler, then we never programmatically schedule.
    if (standardSync.getManual()) {
      return false;
    }

    final boolean timeForNewJob = isTimeForNewJob(previousJobOptional, standardSync);
    return shouldSchedule(previousJobOptional, timeForNewJob);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private boolean shouldSchedule(final Optional<Job> previousJobOptional, final boolean timeForJobNewJob) {
    if (previousJobOptional.isEmpty()) {
      return true;
    }

    final Job previousJob = previousJobOptional.get();
    return switch (previousJob.getStatus()) {
      case CANCELLED, SUCCEEDED, FAILED -> timeForJobNewJob;
      case INCOMPLETE, PENDING, RUNNING -> false;
      default -> throw new IllegalArgumentException("Unrecognized status: " + previousJob.getStatus());
    };
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private boolean isTimeForNewJob(final Optional<Job> previousJobOptional, final StandardSync standardSync) {
    // if non-manual scheduler, and there has never been a previous run, always schedule.
    if (previousJobOptional.isEmpty()) {
      return true;
    }

    final Job previousJob = previousJobOptional.get();

    // if there is an active job, do not start a new one.
    if (!JobStatus.TERMINAL_STATUSES.contains(previousJob.getStatus())) {
      return false;
    }

    final long prevRunStart = previousJob.getStartedAtInSecond().orElse(previousJob.getCreatedAtInSecond());
    final long nextRunStart = prevRunStart + ScheduleHelpers.getIntervalInSecond(standardSync.getSchedule());
    return nextRunStart < timeSupplier.get().getEpochSecond();
  }

}
