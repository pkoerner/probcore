import de.prob.statespace.*
c = api.b_load(dir+"/machines/scheduler.mch")
s = c.statespace
s.explore "root"
s.step 0
idAt0 = s.getCurrentState()
s.step 3
assert s.getCurrentState() == s.states.get("2")
assert s.isExplored(s.states.get("2"))
assert !s.isExplored(s.states.get("5"))
assert s.info.ops.containsKey(new OperationId("1"))
assert !s.isOutEdge(s.getCurrentState(),new OperationId("1"))
s.step 8
idAt8 = s.getCurrentState()
assert idAt0 == idAt8
s.goToState 6
assert s.isExplored(s.states.get("0"))
assert s.isExplored(s.states.get("6"))
assert s.isExplored(s.states.get("root"))
assert !s.isExplored(s.states.get("5"))
assert s.states.get("5") != null
varsAt6 = s.info.getState(s.states.get("6"))
assert varsAt6.get("waiting") == "{}"
assert varsAt6.get("active") == "{PID2}"
assert varsAt6.get("ready") == "{}"
s.addUserFormula("1+1=2")
assert s.info.getVariable(s.getCurrentState(),"1+1=2") == "TRUE"
