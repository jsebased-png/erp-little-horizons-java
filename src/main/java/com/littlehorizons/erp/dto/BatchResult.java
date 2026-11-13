package com.littlehorizons.erp.dto;

import java.util.List;
import java.util.Map;

/**
 * Result of an operation applied to several items (for example joining several clubs).
 * It exists so the view can show which items worked and why the others were rejected.
 *
 * @param accepted ids of the items that were processed
 * @param rejected id of each rejected item with the reason
 */
public record BatchResult(List<Integer> accepted, Map<Integer, String> rejected) {
}
