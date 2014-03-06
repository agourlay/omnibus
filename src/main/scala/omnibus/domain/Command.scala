package omnibus.domain

import java.util.UUID;

// TODO all messages exchanged between actors should have an uuid
abstract class Command (uuid : String = UUID.randomUUID.toString)


