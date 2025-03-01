/*
 * Copyright (c) 2020 New Vector Ltd
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

package im.vector.app.features.room

import com.airbnb.mvrx.MavericksState
import im.vector.app.features.roommemberprofile.RoomMemberProfileArgs
import im.vector.app.features.roomprofile.RoomProfileArgs

data class RequireActiveMembershipViewState(
        val roomId: String? = null
) : MavericksState {

    // No constructor for RoomDetailArgs because of intent for Shortcut

    constructor(args: RoomProfileArgs) : this(roomId = args.roomId)

    constructor(args: RoomMemberProfileArgs) : this(roomId = args.roomId)
}
