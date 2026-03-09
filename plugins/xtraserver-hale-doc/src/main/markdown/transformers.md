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


## sameColumnNonHeadPath (known bug: only head-position path supported)

The same column is mapped to paths whose multiple property is at a non-head position; the transformer incorrectly groups by the first path element only

### Before

 given

```xml
<Join target="[]" axis="parent" join_path="o51001/ref(id:o51001_bwf)::id"/>
<Table table_name="o51001_bwf" target="bu-base:parts/bu-base:BuildingPart/bu-base:dateOfConstruction/@xlink:href" value="bwf"/>
<Table table_name="o51001_bwf" target="bu-base:parts/bu-base:BuildingPart/bu-base:dateOfRenovation/@xlink:href" value="bwf"/>
```

### After

 expected (current wrong output: grouped by first path element only)

```xml
<Join target="[{bu-base}parts]" axis="parent" join_path="o51001/ref(id:o51001_bwf)::id"/>
<Table table_name="o51001_bwf" target="bu-base:parts/bu-base:BuildingPart/bu-base:dateOfConstruction/@xlink:href" value="bwf"/>
<Table table_name="o51001_bwf" target="bu-base:parts/bu-base:BuildingPart/bu-base:dateOfRenovation/@xlink:href" value="bwf"/>
```



