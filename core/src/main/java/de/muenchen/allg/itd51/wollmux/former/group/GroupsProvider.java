/*-
 * #%L
 * WollMux
 * %%
 * Copyright (C) 2005 - 2021 Landeshauptstadt München
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package de.muenchen.allg.itd51.wollmux.former.group;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.muenchen.allg.itd51.wollmux.former.FormularMax4kController;
import de.muenchen.allg.itd51.wollmux.former.model.ID;

/**
 * Ein Ding, das GROUPS über ihre IDs referenziert.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class GroupsProvider implements Iterable<ID>
{
  private Set<ID> groups = new HashSet<>();

  /**
   * Wer wird alles benachrichtigt, wenn Gruppen hinzukommen oder entfernt werden.
   */
  private List<WeakReference<GroupsChangedListener>> listeners = new ArrayList<>();

  /**
   * Der heilige Meister, den wir anbeten.
   */
  private FormularMax4kController formularMax4000;

  /**
   * Erzeugt einen neuen GroupsProvider, der {@link FormularMax4kController#documentNeedsUpdating()}
   * aufruft, wenn etwas an seiner Liste geändert wird.
   */
  public GroupsProvider(FormularMax4kController formularMax4000)
  {
    this.formularMax4000 = formularMax4000;
  }

  /**
   * Liefert die Menge der GROUPS-Werte dieses FormControlModels. Die gelieferte
   * Liste ist keine Referenz auf die interne Datenstruktur. Hinzufügen und Löschen
   * von Einträgen muss über
   * {@link #addGroup(de.muenchen.allg.itd51.wollmux.former.ID)} bzw.
   * {@link #removeGroup(de.muenchen.allg.itd51.wollmux.former.ID)}
   * erfolgen.
   */
  public Set<ID> getGroups()
  {
    return new HashSet<>(groups);
  }

  /**
   * Fügt id zu den GROUPS hinzu, falls noch nicht enthalten.
   */
  public void addGroup(ID id)
  {
    if (!groups.contains(id))
    {
      groups.add(id);
      notifyListeners(id, false);
      formularMax4000.documentNeedsUpdating();
    }

  }

  /**
   * Entfernt id aus GROUPS, falls dort enthalten.
   *
   */
  public void removeGroup(ID id)
  {
    if (groups.remove(id)) groupHasBeenRemoved(id);
    // ACHTUNG! Die remove()-Methode von MyIterator muss mit removeGroup()
    // synchronisiert bleiben
  }

  /**
   * Wird von {@link #removeGroup(ID)} und {@link MyIterator#remove()} aufgerufen.
   */
  private void groupHasBeenRemoved(ID id)
  {
    notifyListeners(id, true);
    formularMax4000.documentNeedsUpdating();
  }

  /**
   * Ersetzt die interne Gruppenliste durch groups. ACHTUNG! FormularMax 4000 wird
   * nicht aufgefordert, das Dokument zu updaten. Diese Methode sollte nur zur
   * Initialisierung des Objekts vor der Verwendung aufgerufen werden.
   */
  public void initGroups(Set<ID> groups)
  {
    this.groups = groups;
  }

  /**
   * Liefert true gdw, die GROUPS-Angabe nicht leer ist.
   */
  public boolean hasGroups()
  {
    return !groups.isEmpty();
  }

  /**
   * listener wird benachrichtigt, wenn Gruppen hinzugefügt oder entfernt werden.
   * ACHTUNG! Es wird nur eine {@link WeakReference} auf listener gehalten! Das
   * heisst der Listener muss auf andere Weise am Leben gehalten werden. Andererseits
   * muss er nicht deregistriert werden, wenn er verschwindet.
   *
   * @param listener
   */
  public void addGroupsChangedListener(GroupsChangedListener listener)
  {
    Iterator<WeakReference<GroupsChangedListener>> iter = listeners.iterator();
    while (iter.hasNext())
    {
      Reference<GroupsChangedListener> ref = iter.next();
      GroupsChangedListener listen2 = ref.get();
      if (listen2 == null)
        iter.remove();
      else if (listen2 == listener) return;
    }
    listeners.add(new WeakReference<GroupsChangedListener>(listener));
  }

  /**
   * Benachrichtigt alle {@link GroupsChangedListener}, dass id hinzugefügt
   * (remove==false) oder entfernt (remove==true) wurde.
   */
  private void notifyListeners(ID id, boolean remove)
  {
    Iterator<WeakReference<GroupsChangedListener>> iter = listeners.iterator();
    while (iter.hasNext())
    {
      Reference<GroupsChangedListener> ref = iter.next();
      GroupsChangedListener listen = ref.get();
      if (listen == null)
        iter.remove();
      else
      {
        if (remove)
          listen.groupRemoved(id);
        else
          listen.groupAdded(id);
      }
    }
  }

  public interface GroupsChangedListener
  {
    public void groupAdded(ID groupID);

    public void groupRemoved(ID groupID);
  }

  @Override
  public Iterator<ID> iterator()
  {
    return new MyIterator();
  }

  private class MyIterator implements Iterator<ID>
  {
    private Iterator<ID> iter;

    private ID lastReturnedID;

    public MyIterator()
    {
      iter = groups.iterator();
    }

    @Override
    public boolean hasNext()
    {
      return iter.hasNext();
    }

    @Override
    public ID next()
    {
      lastReturnedID = iter.next();
      return lastReturnedID;
    }

    @Override
    public void remove()
    {
      iter.remove();
      groupHasBeenRemoved(lastReturnedID);
    }
  }
}
