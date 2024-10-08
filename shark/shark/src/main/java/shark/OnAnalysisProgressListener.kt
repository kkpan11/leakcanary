package shark

import java.util.Locale

/**
 * Reports progress from the [HeapAnalyzer] as they occur, as [Step] values.
 */
fun interface OnAnalysisProgressListener {

  // These steps are defined in the order in which they occur.
  enum class Step {
    PARSING_HEAP_DUMP,
    EXTRACTING_METADATA,
    FINDING_RETAINED_OBJECTS,
    FINDING_PATHS_TO_RETAINED_OBJECTS,
    FINDING_DOMINATORS,
    INSPECTING_OBJECTS,
    COMPUTING_NATIVE_RETAINED_SIZE,
    COMPUTING_RETAINED_SIZE,
    BUILDING_LEAK_TRACES,
    REPORTING_HEAP_ANALYSIS;

    val humanReadableName: String

    init {
      val lowercaseName = name.replace("_", " ")
        .lowercase(Locale.US)
      humanReadableName =
        lowercaseName.substring(0, 1).uppercase(Locale.US) + lowercaseName.substring(1)
    }
  }

  fun onAnalysisProgress(step: Step)

  companion object {

    /**
     * A no-op [OnAnalysisProgressListener]
     */
    val NO_OP = OnAnalysisProgressListener {}
  }
}
