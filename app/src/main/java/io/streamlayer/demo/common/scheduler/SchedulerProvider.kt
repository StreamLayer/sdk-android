package io.streamlayer.demo.common.scheduler

import io.reactivex.Scheduler

interface SchedulerProvider {

    fun trampoline(): Scheduler

    fun newThread(): Scheduler

    fun computation(): Scheduler

    fun io(): Scheduler

    fun ui(): Scheduler
}