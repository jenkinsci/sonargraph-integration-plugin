//Generated from './layers.xml' on 2020-07-07 20:02:15

model "logical"

relaxed artifact controller
{
    include "*/com/hello2morrow/sonargraph/integration/jenkins/controller/**"
}

relaxed artifact persistence
{
    include "*/com/hello2morrow/sonargraph/integration/jenkins/persistence/**"
}

relaxed artifact model
{
    include "*/com/hello2morrow/sonargraph/integration/jenkins/model/**"
}

relaxed artifact tool
{
    include "*/com/hello2morrow/sonargraph/integration/jenkins/tool/**"
}

relaxed artifact foundation
{
    include "*/com/hello2morrow/sonargraph/integration/jenkins/foundation/**"
}