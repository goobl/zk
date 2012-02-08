/* DefaultTreeModel.java

	Purpose:
		
	Description:
		
	History:
		Wed Jan  5 17:37:01 TST 2011, Created by tomyeh

Copyright (C) 2011 Potix Corporation. All Rights Reserved.

*/
package org.zkoss.zul;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.zkoss.lang.Objects;
import org.zkoss.zul.DefaultTreeNode.TreeNodeChildrenList;
import org.zkoss.zul.event.TreeDataEvent;
import org.zkoss.zul.ext.Sortable;
import org.zkoss.zul.ext.Selectable;
import org.zkoss.zul.ext.Openable;

/**
 * A simple tree data model that uses {@link TreeNode} to represent a tree.
 * Thus the whole tree of data must be loaded into memory, and each node
 * must be represented by {@link TreeNode}.
 *
 * <p>If you want to implement a huge tree that only a visible part shall
 * be loaded, it is better to implement it by extending from
 * {@link AbstractTreeModel}.
 *
 * <p>{@link DefaultTreeModel} depends on {@link TreeNode} only.
 * It does not depend on {@link DefaultTreeNode}. However, {@link DefaultTreeNode}
 * depends on {@link DefaultTreeModel}.
 *
 * <p>For introduction, please refer to
 * <a href="http://books.zkoss.org/wiki/ZK_Developer's_Reference/MVC/Model/Tree_Model">ZK Developer's Reference: Tree Model</a>.
 *
 * @author tomyeh
 * @author jumperchen
 * @since 5.0.6
 */
public class DefaultTreeModel<E> extends AbstractTreeModel<TreeNode<E>>
implements Sortable<TreeNode<E>>, Selectable<TreeNode<E>>, Openable<TreeNode<E>>,
java.io.Serializable {

	private static final long serialVersionUID = 20110131094811L;

	private Comparator<TreeNode<E>> _sorting;
	private boolean _sortDir;


	/** Creates a tree with the specified note as the root.
	 * @param root the root (cannot be null).
	 */
	public DefaultTreeModel(TreeNode<E> root) {
		super(root);

		TreeNode<E> parent = root.getParent();
		if (parent != null)
			parent.remove(root);
		root.setModel(this);
	}

	@Override
	public boolean isLeaf(TreeNode<E> node) {
		return node.isLeaf();
	}
	@Override
	public TreeNode<E> getChild(TreeNode<E> parent, int index) {
		return parent.getChildAt(index);
	}
	@Override
	public int getChildCount(TreeNode<E> parent) {
		return parent.getChildCount();
	}
	@Override
	public int getIndexOfChild(TreeNode<E> parent, TreeNode<E> child) {
		return parent.getIndex(child);
	}

	/**
	 * Returns the path from the child, where the path indicates the child is
	 * placed in the whole tree.
	 * @param child the node we are interested in
	 * @since 6.0.0
	 */
	@Override
	public int[] getPath(TreeNode<E> child) {
		final TreeNode<E> root = getRoot();
		List<Integer> p = new ArrayList<Integer>();
		while (root != child) {
			TreeNode<E> parent = child.getParent();
			if (parent != null) {
				for (int i = 0, j = parent.getChildCount(); i < j; i++) {
					if (parent.getChildAt(i) == child) {
						p.add(0, i);
						break;
					}
				}
				child = parent;
			} else break; // ZK-838
		}
		final Integer[] objs = p.toArray(new Integer[p.size()]);
		final int[] path = new int[objs.length];
		for (int i = 0; i < objs.length; i++)
			path[i] = objs[i].intValue();
		return path;
	}

	//-- Sortable --//
	/** Sorts the data.
	 *
	 * @param cmpr the comparator.
	 * @param ascending whether to sort in the ascending order.
	 * It is ignored since this implementation uses cmprt to compare.
	 */
	@Override
	public void sort(Comparator<TreeNode<E>> cmpr, final boolean ascending) {
		_sorting = cmpr;
		_sortDir = ascending;
		TreeNode<E> root = getRoot();
		if (root != null) {
			sort0(root, cmpr);
			fireEvent(TreeDataEvent.STRUCTURE_CHANGED, null, 0, 0);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void sort0(TreeNode<E> node, Comparator<TreeNode<E>> cmpr) {
		if (node.getChildren() == null) return;
		if (node instanceof DefaultTreeNode)
			((TreeNodeChildrenList)node.getChildren()).sort(cmpr);
		else
			Collections.sort(node.getChildren(), cmpr);
		for (TreeNode<E> child: node.getChildren())
			sort0(child, cmpr);
	}
	
	//Selectable//
	/**
	 * Returns the collection of the selected {@link TreeNode}.
	 */
	public Set<TreeNode<E>> getSelection() {
		final Set<TreeNode<E>> selected = new LinkedHashSet<TreeNode<E>>();
		int[][] paths = getSelectionPaths();
		if (paths != null)
			for (int i = 0; i < paths.length; i++)
				selected.add(getChild(paths[i]));
		return selected;
	}
	@Override
	public void setSelection(Collection<? extends TreeNode<E>> selection) {
		clearSelection();
		for (final TreeNode<E> node: selection)
			addToSelection(node);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isSelected(Object child) {
		if (child instanceof TreeNode) {
			final int[] path = getPath((TreeNode)child);
			if (path != null && path.length > 0)
				return isPathSelected(path);
		}
		return false;
	}
	@Override
	public void addToSelection(TreeNode<E> child) {
		final int[] path = getPath(child);
		if (path != null && path.length > 0)
			addSelectionPath(path);
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean removeFromSelection(Object child) {
		if (child instanceof TreeNode) {
			final int[] path = getPath((TreeNode)child);
			if (path != null && path.length > 0)
				return removeSelectionPath(path);
		}
		return false;
	}

	//Openable//
	@Override
	public void setOpen(TreeNode<E> child, boolean open) {
		final int[] path = getPath(child);
		if (path != null && path.length > 0) {
			if (open)
				addOpenPath(path);
			else
				removeOpenPath(path);
		}
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public boolean isOpen(Object child) {
		if (child instanceof TreeNode) {
			final int[] path = getPath((TreeNode)child);
			if (path != null && path.length > 0)
				return isPathOpened(path);
		}
		return false;
	}

	//For Backward Compatibility//
	/** @deprecated As of release 6.0.0, replaced with {@link #addToSelection}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addSelection(Object obj) {
		if (obj instanceof TreeNode)
			addToSelection((TreeNode)obj);
	}
	/** @deprecated As of release 6.0.0, replaced with {@link #removeFromSelection}.
	 */
	public void removeSelection(Object obj) {
		removeFromSelection(obj);
	}

	public String getSortDirection(Comparator<TreeNode<E>> cmpr) {
		if (Objects.equals(_sorting, cmpr))
			return _sortDir ?
					"ascending" : "descending";
		return "natural";	
	}
}
