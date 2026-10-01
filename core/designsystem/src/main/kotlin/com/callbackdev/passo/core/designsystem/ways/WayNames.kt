package com.callbackdev.passo.core.designsystem.ways

import com.callbackdev.passo.core.model.WayId

/** A way's name and where it runs, as string resources. */
fun routeOf(id: WayId): Pair<Int, Int> = wayNameRes(id) to wayRouteRes(id)
