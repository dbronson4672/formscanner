# Form Template XML (`*.xtmpl`)

This project persists scanning templates as XML files with the `.xtmpl` extension. The Java classes in `formscanner-api` (`FormTemplate`, `FormQuestion`, `FormArea`, and `FormPoint`) are responsible for serializing and deserializing this format. No XML schema ships with the repository, so this document captures the structure expected by the codebase (current template version `2.1`) and links to a generated XML Schema Definition (XSD) that tooling can validate against.

## Document layout

```text
template
├─ crop
├─ rotation
├─ corners
│  └─ corner (×4) → point
└─ fields
   └─ group (0..*)
      ├─ question (0..*)
      │  └─ values → value (1..*) → point
      └─ area (0..*)
         └─ corners → corner (×4) → point
```

### `template`

| Attribute    | Type    | Required | Notes                                                                 |
|--------------|---------|----------|-----------------------------------------------------------------------|
| `version`    | string  | yes      | Current writer sets `2.1`.                                            |
| `threshold`  | int     | no       | Pixel threshold (0–255 recommended).                                  |
| `density`    | int     | no       | Fill ratio percentage (0–100 recommended).                            |

Children appear in the fixed order `crop`, `rotation`, `corners`, `fields`.

### `crop`

Contains four required non-negative integer attributes: `top`, `left`, `right`, `bottom`. They describe the padding (in pixels) trimmed from each edge before processing.

### `rotation`

Single `angle` attribute (floating point). Stored in radians and represents the rotation applied when the template was detected.

### `corners`

Optional `type` attribute maps to `CornerType` (`ANGULAR` or `ROUND`). Contains four `corner` elements:

| Attribute  | Allowed values                            |
|------------|-------------------------------------------|
| `position` | `TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT` |

Each `corner` wraps a `point`.

### `point`

Leaf element with numeric attributes `x` and `y` (double precision coordinates in image pixels).

### `fields`

Attributes:

| Attribute | Type     | Required | Notes                                                          |
|-----------|----------|----------|----------------------------------------------------------------|
| `groups`  | boolean  | yes      | When `true`, UI exposes group management.                      |
| `size`    | int      | no       | Marker size used for bubble detection (pixels).                |
| `shape`   | enum     | no       | `SQUARE` or `CIRCLE`, matching `Constants.ShapeType`.          |

Contains zero or more `group` children.

### `group`

`name` attribute is required (empty names default to the special `EMPTY` group at runtime). A group can mix `question` and `area` children in any order.

### `question`

Represents a multiple-choice block.

| Attribute         | Type    | Required | Notes                                                                                             |
|-------------------|---------|----------|---------------------------------------------------------------------------------------------------|
| `type`            | enum    | yes      | `QUESTIONS_BY_ROWS`, `QUESTIONS_BY_COLS`, or `RESPONSES_BY_GRID` (`Constants.FieldType`).         |
| `question`        | string  | yes      | UI label.                                                                                         |
| `multiple`        | boolean | yes      | `true` when multiple responses may be filled.                                                     |
| `rejectMultiple`  | boolean | yes      | If `true`, the engine flags the answer invalid when several marks trigger.                        |
| `orientation`     | enum    | no       | Deprecated alias for `type`; still parsed for backward compatibility.                             |

Each `question` owns a single `values` container. Inside, one or more `value` elements define potential answers; their order is preserved when writing.

### `value`

`response` attribute stores the answer label. Body contains a single `point` locating the bubble center (coordinates in original scan space).

### `area`

Used for non-question regions such as barcodes.

| Attribute | Type | Required | Notes                                                                    |
|-----------|------|----------|--------------------------------------------------------------------------|
| `type`    | enum | yes      | `BARCODE` today, but the parser accepts any `FieldType` value.           |
| `name`    | string | yes    | Display label.                                                           |

Structure mirrors the top-level `corners` block (`corners` → four `corner` elements → `point`).

## Example (`version="2.1"`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<template version="2.1" threshold="127" density="40">
  <crop top="0" left="0" right="0" bottom="0"/>
  <rotation angle="0.0"/>
  <corners type="ANGULAR">
    <corner position="TOP_LEFT"><point x="153.78" y="153.5"/></corner>
    <corner position="TOP_RIGHT"><point x="2324.62" y="153.5"/></corner>
    <corner position="BOTTOM_LEFT"><point x="153.78" y="3353.5"/></corner>
    <corner position="BOTTOM_RIGHT"><point x="2324.62" y="3353.5"/></corner>
  </corners>
  <fields groups="true" shape="SQUARE" size="20">
    <group name="Section A">
      <question type="QUESTIONS_BY_ROWS" question="Question 01" multiple="false" rejectMultiple="false">
        <values>
          <value response="Response 01"><point x="800.0" y="1300.0"/></value>
          <value response="Response 02"><point x="925.0" y="1300.0"/></value>
        </values>
      </question>
      <area type="BARCODE" name="Barcode">
        <corners>
          <corner position="TOP_LEFT"><point x="800.0" y="2100.0"/></corner>
          <corner position="TOP_RIGHT"><point x="1200.0" y="2100.0"/></corner>
          <corner position="BOTTOM_LEFT"><point x="800.0" y="2300.0"/></corner>
          <corner position="BOTTOM_RIGHT"><point x="1200.0" y="2300.0"/></corner>
        </corners>
      </area>
    </group>
  </fields>
</template>
```

## Compatibility notes

- Templates created prior to 2.x stored `shape` and `size` attributes on the root element and omitted explicit `<group>` wrappers. The parser still accepts those files, but new exports always follow the structure above.
- Group-less templates are stored under the implicit group name `EMPTY`.
- Optional attributes default to the constants in `FormTemplate` if not provided.

## XML schema

The accompanying schema `docs/form-template.xsd` formalizes the current layout for tooling. Validate templates with:

```bash
xmlstarlet val --xsd docs/form-template.xsd my-template.xtmpl
```

Keep the schema updated if new fields are introduced in `FormTemplate`.
