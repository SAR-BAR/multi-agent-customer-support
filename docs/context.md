# Customer support

Glossary for the multi-agent customer support system. Terms only; no implementation details.

## Terms

**Conversation**
One chat session with a signed-in customer, from their first message to the end of the session. A conversation can contain several cases, or none.
_Avoid_: chat, session, ticket

**Case**
Everything a customer raises about one order, e.g. "O100 arrived late and I was charged a delivery fee I shouldn't pay" is one case with two problems. Problems about different orders are different cases. A follow-up about the same order reopens its existing case, which is what makes repeat contacts countable.
_Avoid_: ticket, issue, request

**Problem**
One thing the customer says is wrong within a case, e.g. "late delivery" or "wrong delivery fee". A case has one or more problems.
_Avoid_: issue, complaint, intent

**General message**
A message that isn't about any order, e.g. a policy question, an account change, feedback, or something off-topic. It never opens a case.
_Avoid_: non-order case, generic case

**Case board**
The shared record of everything that happens on one case. Agents read it and add posts to it; each case has exactly one.
_Avoid_: blackboard (that's the pattern's name, not the thing), case file, case log

**Post**
One entry on a case board, written by one author: an agent, the system, or a human.
_Avoid_: message, event

**Facts**
The post the system writes first on every case board: the verified numbers and dates for that order. Agents judge facts; they never produce them.
_Avoid_: data, context

**Proposal**
A specialist's suggested fix for one or more problems, with the evidence it relies on. A proposal is never acted on by itself.
_Avoid_: action, recommendation, decision

**Withdrawal**
A specialist taking back its own proposal, usually because another proposal already covers the same fix.

**Handoff**
A specialist passing a problem to the other specialist because it was routed to the wrong one.
_Avoid_: transfer, reassignment

**Challenge**
Risk asking a specialist for specific evidence behind a proposal.
_Avoid_: question, objection

**Verdict**
Risk's final view of a proposal: allow, review, or high risk.

**Decision**
The system's final outcome for a case: execute, escalate, ask the customer, or deny. Only the system makes decisions.
_Avoid_: verdict, resolution

**Escalation**
Handing a case to a human support agent, with a handoff brief.

**Precedent**
A past human decision on an escalated case, which specialists can cite for similar cases.
