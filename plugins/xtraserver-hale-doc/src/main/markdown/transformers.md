# MultiJoin

bla bla

## sameColumnDifferentPaths

The same column in a joined table is mapped to multiple paths with different roots

### Before

 given

```xml
<Join target="[]" axis="parent" join_path="o51001/ref(id:o51001_bwf)::id"/>
<Table table_name="o51001_bwf" target="bu-base:buildingNature/@xlink:href" value="bwf"/>
<Table table_name="o51001_bwf" target="bu-base:currentUse/bu-base:CurrentUse/bu-base:currentUse/@xlink:href" value="bwf"/>
```

### After

 expected

```xml
<Join target="[{bu-base}buildingNature]" axis="parent" join_path="o51001/ref(id:o51001_bwf)::id"/>
<Table table_name="o51001_bwf" target="bu-base:buildingNature/@xlink:href" value="bwf"/>

<Join target="[{bu-base}currentUse]" axis="parent" join_path="o51001/ref(id:o51001_bwf)::id"/>
<Table table_name="o51001_bwf" target="bu-base:currentUse/bu-base:CurrentUse/bu-base:currentUse/@xlink:href" value="bwf"/>
```



