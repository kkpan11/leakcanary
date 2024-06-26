package shark

import shark.HeapObject.HeapClass
import shark.Reference.LazyDetails
import shark.ReferenceLocationType.STATIC_FIELD
import shark.ReferencePattern.StaticFieldPattern
import shark.ValueHolder.ReferenceHolder

class ClassReferenceReader(
  graph: HeapGraph,
  referenceMatchers: List<ReferenceMatcher>
) : ReferenceReader<HeapClass> {
  private val staticFieldNameByClassName: Map<String, Map<String, ReferenceMatcher>>

  init {
    val staticFieldNameByClassName = mutableMapOf<String, MutableMap<String, ReferenceMatcher>>()
    referenceMatchers.filterFor(graph).forEach { referenceMatcher ->
      val pattern = referenceMatcher.pattern
      if (pattern is StaticFieldPattern) {
        val mapOrNull = staticFieldNameByClassName[pattern.className]
        val map = if (mapOrNull != null) mapOrNull else {
          val newMap = mutableMapOf<String, ReferenceMatcher>()
          staticFieldNameByClassName[pattern.className] = newMap
          newMap
        }
        map[pattern.fieldName] = referenceMatcher
      }
    }
    this.staticFieldNameByClassName = staticFieldNameByClassName
  }

  override fun read(source: HeapClass): Sequence<Reference> {
    val ignoredStaticFields = staticFieldNameByClassName[source.name] ?: emptyMap()

    return source.readStaticFields().mapNotNull { staticField ->
      // not non null: no null + no primitives.
      if (!staticField.value.isNonNullReference) {
        return@mapNotNull null
      }
      val fieldName = staticField.name
      if (
      // Android noise
        fieldName == "\$staticOverhead" ||
        // Android noise
        fieldName == "\$classOverhead" ||
        // JVM noise
        fieldName == "<resolved_references>"
      ) {
        return@mapNotNull null
      }

      // Note: instead of calling staticField.value.asObjectId!! we cast holder to ReferenceHolder
      // and access value directly. This allows us to avoid unnecessary boxing of Long.
      val valueObjectId = (staticField.value.holder as ReferenceHolder).value
      val referenceMatcher = ignoredStaticFields[fieldName]

      if (referenceMatcher is IgnoredReferenceMatcher) {
        null
      } else {
        val sourceObjectId = source.objectId
        Reference(
          valueObjectId = valueObjectId,
          isLowPriority = referenceMatcher != null,
          lazyDetailsResolver = {
            LazyDetails(
              name = fieldName,
              locationClassObjectId = sourceObjectId,
              locationType = STATIC_FIELD,
              isVirtual = false,
              matchedLibraryLeak = referenceMatcher as LibraryLeakReferenceMatcher?,
            )
          }
        )
      }
    }
  }
}
