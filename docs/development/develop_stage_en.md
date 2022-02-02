# Development mode

For local testing of some dogus, it may be necessary to put the CAS into development mode.
This causes all applications to be able to authenticate via the CAS, even if they are not configured there.
For this, the stage of the EcoSystem must be set to `development` with the command 
`etcdctl set /config/_global/stage development`. After that, the dogu has to be restarted.
