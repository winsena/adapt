* Main.hs

A set of tests for Ingest, Trint, and ingestd.

* provn-files/

These example .provn files are taken from the infoleak example generated by
SPADE.  Each file can be ingested by Trint and translated to our CDM-inspired
internal data format.  Notice the .provn files are not complete - not all
possible Prov-N is generated by SPADE and not all statements generated by SPADE
have been 'plucked' from an available source and added to this directory.

# Manual Confirmation of Provenance Translation

At present, each prov statement is translated independently, so it suffices to
consider the translations in isolation.  To compare the internal form to the
input prov use trint's ast feature:

```
    Trint -a wasGeneratedBy.prov && cat wasGeneratedBy.prov.trint
```

In the generated .trint file, you can see there is a new node declaration and
two new edges.  The node declares the event while the edges connect the event to
the source and destination.